module io.kani.gui {
    requires kotlin.stdlib;

    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;

    requires org.slf4j;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    opens io.kani.gui to javafx.graphics, javafx.fxml;
    opens io.kani.gui.ui to javafx.graphics, javafx.fxml;
}
