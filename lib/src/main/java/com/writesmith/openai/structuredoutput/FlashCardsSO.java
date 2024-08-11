package com.writesmith.openai.structuredoutput;

import com.oaigptconnector.model.JSONSchema;
import com.oaigptconnector.model.JSONSchemaParameter;

import java.util.List;

@JSONSchema(name = "Create_Flashcards", functionDescription = "Create flashcards from the user's input.", strict = JSONSchema.NullableBool.TRUE)
public class FlashCardsSO {

    public static class FlashCard {

        @JSONSchemaParameter
        private String front;

        @JSONSchemaParameter
        private String back;

        public FlashCard() {

        }

        public FlashCard(String front, String back) {
            this.front = front;
            this.back = back;
        }

        public String getFront() {
            return front;
        }

        public String getBack() {
            return back;
        }

    }

    @JSONSchemaParameter
    private List<FlashCard> flashCards;

    public FlashCardsSO() {

    }

    public FlashCardsSO(List<FlashCard> flashCards) {
        this.flashCards = flashCards;
    }

    public List<FlashCard> getFlashCards() {
        return flashCards;
    }

}
