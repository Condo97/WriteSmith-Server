package com.writesmith.core.service.endpoints;

import com.writesmith.core.service.ResponseStatus;
import com.writesmith.core.service.request.AuthRequest;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.GetSubscriptionOfferingResponse;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.database.dao.pooled.*;
import com.writesmith.database.model.objects.*;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import javax.security.sasl.AuthenticationException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GetSubscriptionOfferingEndpoint {

    public static BodyResponse getSubscriptionOffering(AuthRequest request) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, DBSerializerPrimaryKeyMissingException, AuthenticationException, DBObjectNotFoundFromQueryException {
        // 1. Validate authToken -> get user_id
        User_AuthToken u_aT;
        try {
            u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());
        } catch (DBObjectNotFoundFromQueryException e) {
            throw new AuthenticationException("Could not find authToken.");
        }

        Integer userId = u_aT.getUserID();

        // 2. Find the active offering
        SubscriptionOffering activeOffering = SubscriptionOfferingDAOPooled.getActive();
        if (activeOffering == null) {
            return new BodyResponse(ResponseStatus.SUCCESS, null);
        }

        // 3. Check for existing assignment
        UserOfferingAssignment assignment = UserOfferingAssignmentDAOPooled.getByUserAndOffering(userId, activeOffering.getId());

        // Fetch all groups for this offering (needed for both assignment lookup and weighted selection)
        List<SubscriptionOfferingGroup> groups = SubscriptionOfferingGroupDAOPooled.getByOfferingId(activeOffering.getId());
        if (groups.isEmpty()) {
            return new BodyResponse(ResponseStatus.SUCCESS, null);
        }

        Integer assignedGroupId;
        String groupName;

        if (assignment != null) {
            // Use existing assignment
            assignedGroupId = assignment.getOfferingGroupId();
            groupName = findGroupName(groups, assignedGroupId);
        } else {
            // 4. No existing assignment -- assign randomly by weight
            SubscriptionOfferingGroup selectedGroup = selectGroupByWeight(groups);
            assignedGroupId = selectedGroup.getId();
            groupName = selectedGroup.getGroupName();

            // Persist the assignment; handle race condition where two concurrent requests
            // for the same user both try to insert (UNIQUE constraint will reject the second)
            try {
                UserOfferingAssignment newAssignment = new UserOfferingAssignment(userId, activeOffering.getId(), assignedGroupId);
                UserOfferingAssignmentDAOPooled.insert(newAssignment);
            } catch (SQLException e) {
                if (e instanceof SQLIntegrityConstraintViolationException || (e.getMessage() != null && e.getMessage().contains("Duplicate entry"))) {
                    // Another concurrent request already assigned this user; read the existing assignment
                    assignment = UserOfferingAssignmentDAOPooled.getByUserAndOffering(userId, activeOffering.getId());
                    if (assignment != null) {
                        assignedGroupId = assignment.getOfferingGroupId();
                        groupName = findGroupName(groups, assignedGroupId);
                    }
                } else {
                    throw e;
                }
            }
        }

        // 5. Fetch the group's products and copy
        List<SubscriptionOfferingGroupProduct> products = SubscriptionOfferingGroupProductDAOPooled.getByGroupId(assignedGroupId);
        SubscriptionOfferingGroupCopy copy = SubscriptionOfferingGroupCopyDAOPooled.getByGroupId(assignedGroupId);

        // 6. Build response
        List<GetSubscriptionOfferingResponse.SubscriptionProduct> subscriptionProducts = new ArrayList<>();
        for (SubscriptionOfferingGroupProduct p : products) {
            subscriptionProducts.add(new GetSubscriptionOfferingResponse.SubscriptionProduct(
                    p.getProductId(),
                    p.getType(),
                    p.getFallbackDisplayPrice(),
                    p.getPosition() != null ? p.getPosition() : 0,
                    p.getIsDefault() != null && p.getIsDefault(),
                    p.getBadgeText(),
                    p.getSubtitleText()
            ));
        }

        GetSubscriptionOfferingResponse.SubscriptionCopy subscriptionCopy = new GetSubscriptionOfferingResponse.SubscriptionCopy(
                copy.getHeaderTitle(),
                copy.getHeaderSubtitle(),
                copy.getCtaButtonText(),
                copy.getSupportingText()
        );

        GetSubscriptionOfferingResponse response = new GetSubscriptionOfferingResponse(
                activeOffering.getOfferingId(),
                groupName,
                subscriptionProducts,
                subscriptionCopy
        );

        return BodyResponseFactory.createSuccessBodyResponse(response);
    }

    private static SubscriptionOfferingGroup selectGroupByWeight(List<SubscriptionOfferingGroup> groups) {
        double totalWeight = 0.0;
        for (SubscriptionOfferingGroup group : groups) {
            totalWeight += group.getWeight();
        }

        double random = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0.0;

        for (SubscriptionOfferingGroup group : groups) {
            cumulative += group.getWeight();
            if (random < cumulative) {
                return group;
            }
        }

        // Fallback (should not happen due to floating point, but just in case)
        return groups.get(groups.size() - 1);
    }

    private static String findGroupName(List<SubscriptionOfferingGroup> groups, Integer groupId) {
        for (SubscriptionOfferingGroup group : groups) {
            if (group.getId().equals(groupId)) {
                return group.getGroupName();
            }
        }
        return "unknown";
    }

}
