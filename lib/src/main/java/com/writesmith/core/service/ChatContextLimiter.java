package com.writesmith.core.service;

import com.writesmith.Constants;
import com.writesmith.database.model.objects.Chat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatContextLimiter {

//    public static class LimitedChats {
//
//        private List<Chat> chats;
//        private boolean removedImage;
//        private boolean removedImageURL;
//
//        public LimitedChats(List<Chat> chats, boolean removedImage, boolean removedImageURL) {
//            this.chats = chats;
//            this.removedImage = removedImage;
//            this.removedImageURL = removedImageURL;
//        }
//
//        public List<Chat> getChats() {
//            return chats;
//        }
//
//        public boolean removedImage() {
//            return removedImage;
//        }
//
//        public boolean removedImageURL() {
//            return removedImageURL;
//        }
//
//    }

    public static List<Chat> getLimitedChats(List<Chat> chats, int characterLimit) {
        List<Chat> limitedChats = new ArrayList<>();

        int totalChars = 0;

        // Loop through Chats asc, if totalChars is less than characterLimit add Chat to limitedChats removing image if necessary and setting removedImage accordingly
        for (int i = 0; i < chats.size(); i++) {
            if (totalChars < characterLimit) {
                // Add Chat to limitedChats
                limitedChats.add(chats.get(i));

                // If Chat contains text, add to totalChars
                if (chats.get(i).getText() != null && !chats.get(i).getText().isEmpty()) {
                    totalChars += chats.get(i).getText().length();
                }
            } else {
                break;
            }
        }

        // Return LimitedChats with limitedChats, removedImage, and removedImageURL
        return limitedChats;
    }

//    public static List<Chat> getLimitedChats(List<Chat> chats, int characterLimit, boolean skipImages) {
//        List<Chat> limitedChats = new ArrayList<>();
//
//        int totalChars = 0;
//
//        // Loop through Chats asc, if totalChars is less than characterLimit add Chat to limitedChats removing image if necessary and setting removedImage accordingly
//        for (int i = 0; i < chats.size(); i++) {
//            if (totalChars < characterLimit) {
//                // Add Chat to limitedChats
//                limitedChats.add(chats.get(i));
//
//                // If Chat contains text, add to totalChars
//                if (chats.get(i).getText() != null && !chats.get(i).getText().isEmpty()) {
//                    totalChars += chats.get(i).getText().length();
//                }
//
//                if (!skipImages) {
//                    // If Chat contains image data either don't include and set removedImage to true or add with Image_Token_Count
//                    if (chats.get(i).getImageData() != null && !chats.get(i).getImageData().isEmpty()) {
//                        totalChars += Constants.Image_Token_Count;
//                    }
//
//                    // If chat contains image URL either don't include and set removedImageURL to true or add with Image_Token_Count
//                    if (chats.get(i).getImageURL() != null && !chats.get(i).getImageURL().isEmpty()) {
//                        totalChars += Constants.Image_Token_Count;
//                    }
//                }
//            } else {
//                break;
//            }
//        }
//
//        // Return LimitedChats with limitedChats, removedImage, and removedImageURL
//        return limitedChats;
//
////        int countToRemove = 0;
////
////        // Starting with the most recent Chat, remove characters of each chat from characterLimit until it is less than 0, then count how many older chats need to be removed
////        for (int i = chats.size() - 1; i >= 0; i--) {
////            if (characterLimit <= 0 || (chats.get(i).getText() == null && chats.get(i).getImageData() == null && chats.get(i).getImageURL() == null)) {
////                // If characterLimit is <= 0 or chat text, image, or image URL is null, remove from chats array
////                chats.remove(i);
////            } else {
////                // If text is not null and not empty, subtract its length from characterLimit
////                if (chats.get(i).getText() != null && !chats.get(i).getText().isEmpty()) {
////                    characterLimit -= chats.get(i).getText().length();
////                }
////
////                // If image data is not null and not empty, subtract Image_Token_Count from characterLimit
////                if (chats.get(i).getImageData() != null && !chats.get(i).getImageData().isEmpty()) {
////                    characterLimit -= Constants.Image_Token_Count;
////                }
////
////                // If image URL is not null and not empty, subtract Image_Token_Count from characterLimit
////                if (chats.get(i).getImageURL() != null && !chats.get(i).getImageURL().isEmpty()) {
////                    characterLimit -= Constants.Image_Token_Count;
////                }
////            }
////        }
////
////        return chats;
//    }
//
//    public static boolean imageDataOrURLInLimitedChats(List<Chat> chats, int characterLimit) {
//        int totalChars = 0;
//
//        for (int i = chats.size() - 1; i >= 0; i--) {
//            if (totalChars < characterLimit) {
//                if (chats.get(i).getText() != null && !chats.get(i).getText().isEmpty())
//                    totalChars += chats.get(i).getText().length();
//
//                if (chats.get(i).getImageData() != null && !chats.get(i).getImageData().isEmpty())
//                    return true;
//            } else {
//                break;
//            }
//        }
//
//        return false;
//    }

}
