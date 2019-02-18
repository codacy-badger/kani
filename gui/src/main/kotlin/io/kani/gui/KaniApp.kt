package io.kani.gui

import io.kani.applock.AppLocker
import io.kani.applock.Response
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import java.nio.file.Paths


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
        val l = AppLocker("test", Paths.get("C:/locks"))
        println(l.lock())

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
