//package io.kani.ui
//
//import com.google.inject.Injector
//import javafx.fxml.FXMLLoader
//import javafx.scene.Parent
//import mu.KLogging
//import mu.KotlinLogging
//import javax.inject.Inject
//import kotlin.properties.ReadOnlyProperty
//import kotlin.reflect.KProperty
//
//
//inline fun <reified T : Any> FxComponent.inject() = object : ReadOnlyProperty<FxComponent, T> {
//    override fun getValue(thisRef: FxComponent, property: KProperty<*>): T = injector.getInstance(T::class.java)
//}
//
//abstract class FxComponent {
//    companion object : KLogging()
//
//    @Inject
//    lateinit var injector: Injector
//
//    abstract val root: Parent
//    protected val fxml: FXMLLoader by inject()
//
//}
//
//private val logger = KotlinLogging.logger {}
//
///**
// * Helper delegate for [fxml] field initialization
// *
// * @param file path to .fxml resource
// */
//fun Parent.fxml(file: String) = object : ReadOnlyProperty<Parent, Parent> {
//    // create value so loadFXML is not deferred until the first access to the property
//    private val value: Parent = loadFXML(file)
//
//    override fun getValue(thisRef: Parent, property: KProperty<*>): Parent = value
//
//    private fun loadFXML(fxmlFile: String): Parent {
//        val loader = FXMLLoader(this@fxml::class.java.getResource(fxmlFile))
//        loader.setControllerFactory { this@fxml }
//        // this tells FXMLLoader, that it must use provided instance as root object
//        loader.setRoot(this@fxml)
//
//        try {
//            return loader.load<Parent>()
//        } catch (e: Exception) {
//            logger.error(e) { "Error while loading FXML `$fxmlFile`" }
//            throw e
//        }
//    }
//}
