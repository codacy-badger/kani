package io.kani.gui

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.slf4j.LoggerFactory


class KaniApp : Application() {
    private val logger = LoggerFactory.getLogger(KaniApp::class.java)
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(KaniApp::class.java, *args)
        }
    }

    override fun start(stage: Stage) {
        logger.info("Application start")

        val mainWindow = FXMLLoader.load<VBox>(this::class.java.getResource("/fxml/MainWindow.fxml"))
        val scene = Scene(mainWindow)
        stage.scene = scene

        stage.title = "Kani"
        stage.show()
    }

    override fun stop() {
        logger.info("Application stop")
        super.stop()
    }
}
