module ske.client {
    requires javafx.controls;
    requires javafx.fxml;


    opens ske.client to javafx.fxml;
    exports ske.client;
}