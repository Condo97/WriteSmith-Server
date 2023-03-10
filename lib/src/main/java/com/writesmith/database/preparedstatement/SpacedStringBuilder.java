package com.writesmith.database.preparedstatement;

import java.util.ArrayList;
import java.util.Arrays;

public class SpacedStringBuilder {
    private final String SPACE = " ";
    private final String COMMA_SEPARATION = ", ";
    private final String ENCLOSURE_OPEN = "(";
    private final String ENCLOSURE_CLOSE = ")";

    private ArrayList<String> strings;

    public SpacedStringBuilder() {
        strings = new ArrayList<>();
    }

    public SpacedStringBuilder(String... strings) {
        this.strings = new ArrayList<>(Arrays.asList(strings));
    }

    public SpacedStringBuilder(ArrayList<String> strings) {
        this.strings = new ArrayList<>(strings);
    }

    public SpacedStringBuilder append(String... strings) {
        this.strings.addAll(Arrays.asList(strings));
        return this;
    }

    public String toString() {
        return strings.toString();
    }

    public String toCommaSeparatedString() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < strings.size(); i++) {
            sb.append(strings.get(i));

            if (i < strings.size() - 1) sb.append(COMMA_SEPARATION);
        }

        return sb.toString();
    }

    public String toEnclosedCommaSeparatedString() {
        StringBuilder sb = new StringBuilder();

        sb.append(ENCLOSURE_OPEN);
        sb.append(toCommaSeparatedString());
        sb.append(ENCLOSURE_CLOSE);

        return sb.toString();
    }
}
