//package io.kani.ui
//
//import javafx.fxml.FXMLLoader
//import javafx.scene.Parent
//import mu.KLogging
//import javax.inject.Inject
//
//class FxComponentFactory @Inject constructor(private val loader: FXMLLoader) {
//    companion object : KLogging()
//
//    fun <T : Parent> load(fxmlFile: String, root: T): T {
//        loader.setControllerFactory { root }
//        loader.setRoot(root)
//
//        try {
//            return loader.load<T>(root::class.java.getResourceAsStream(fxmlFile))
//        } catch (e: Exception) {
//            logger.error(e) { "Error while loading FXML `$fxmlFile`" }
//            throw e
//        }
//    }
//}
