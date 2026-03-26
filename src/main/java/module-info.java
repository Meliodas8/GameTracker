module dev.manel.gametracker {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    opens dev.manel.gametracker to javafx.graphics, javafx.fxml;
    opens dev.manel.gametracker.ui to javafx.fxml;
    opens dev.manel.gametracker.core.model to com.google.gson, javafx.base;
    opens dev.manel.gametracker.session to com.google.gson;
    opens dev.manel.gametracker.core.config to com.google.gson;
    opens dev.manel.gametracker.autostart to javafx.base;
}