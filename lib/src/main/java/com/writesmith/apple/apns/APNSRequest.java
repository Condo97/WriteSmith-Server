package com.writesmith.apple.apns;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class APNSRequest {

    public static class APS {

        public static class Alert {

            private String title;
            private String subtitle;
            private String body;
            private String sound;

            @JsonProperty("apns-priority")
            private Integer apnsPriority;

            public Alert() {

            }

            public Alert(String title, String subtitle, String body, String sound, Integer apnsPriority) {
                this.title = title;
                this.subtitle = subtitle;
                this.body = body;
                this.sound = sound;
                this.apnsPriority = apnsPriority;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getSubtitle() {
                return subtitle;
            }

            public void setSubtitle(String subtitle) {
                this.subtitle = subtitle;
            }

            public String getBody() {
                return body;
            }

            public void setBody(String body) {
                this.body = body;
            }

            public String getSound() {
                return sound;
            }

            public void setSound(String sound) {
                this.sound = sound;
            }

            public Integer getApnsPriority() {
                return apnsPriority;
            }

            public void setApnsPriority(Integer apnsPriority) {
                this.apnsPriority = apnsPriority;
            }

        }

        private Alert alert;

        public APS() {

        }

        public APS(Alert alert) {
            this.alert = alert;
        }

        public Alert getAlert() {
            return alert;
        }

        public void setAlert(Alert alert) {
            this.alert = alert;
        }

    }

    private APS aps;

    public APNSRequest() {

    }

    public APNSRequest(APS aps) {
        this.aps = aps;
    }

    public APS getAps() {
        return aps;
    }

    public void setAps(APS aps) {
        this.aps = aps;
    }

}
