module io.kani {
    requires kotlin.stdlib;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    opens io.kani to javafx.graphics, javafx.fxml;
    opens io.kani.ui to javafx.graphics, javafx.fxml;
}
