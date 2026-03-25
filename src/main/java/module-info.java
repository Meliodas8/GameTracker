module dev.manel.gametracker {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    opens dev.manel.gametracker to javafx.graphics;
    opens dev.manel.gametracker.core.model to com.google.gson;
    opens dev.manel.gametracker.session to com.google.gson;
}