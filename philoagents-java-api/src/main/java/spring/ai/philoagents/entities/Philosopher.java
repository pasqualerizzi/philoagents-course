    package spring.ai.philoagents.entities;

    public class Philosopher {
        private String id;
        private String name;
        private String perspective;
        private String style;

        public Philosopher() {}

        public Philosopher(String id, String name, String perspective, String style) {
            this.id = id;
            this.name = name;
            this.perspective = perspective;
            this.style = style;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPerspective() {
            return perspective;
        }

        public void setPerspective(String perspective) {
            this.perspective = perspective;
        }

        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }
    }