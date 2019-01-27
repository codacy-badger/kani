//package io.kani.ui
//
//import com.google.inject.Injector
//import javafx.fxml.FXMLLoader
//import javafx.scene.Node
//import mu.KotlinLogging
//import kotlin.properties.ReadOnlyProperty
//import kotlin.reflect.KProperty
//
//private val logger = KotlinLogging.logger {}
//
///**
// * Utility interface to leverage FXML and Java/Kotlin class.
// *
// * The goal is to unite (make mapping) between JavaFX view/controller,
// * so the end user has more flexibility and easier FXML instantiation.
// *
// * Usage:
// * ```
// * class Window : VBox(), FXMLComponent {
// *      override val fxml: FXMLLoader by fxml("<fxml>")
// *      val someObject: Node by fxid()
// * }
// * ```
// *
// * FXML's top level node must be `fx:root` with required attribute `type`,
// * `type` must match declared superclass, for example:
// * ```
// * <fx:root type="VBox"
// *     xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
// *     fx:controller="example.FxmlController">
// *    <children/>
// * <fx:root/>
// * ```
// */
////interface FXMLComponent {
////    val fxml: FXMLLoader
////    val injector: Injector
////
////
////    /**
////     * Helper delegate for [fxml] field initialization
////     *
////     * @param file path to .fxml resource
////     * @param useFxmlController indicates that .fxml file has set fx:controller property
////     */
////    fun fxml(file: String, useFxmlController: Boolean = true) = object : ReadOnlyProperty<FXMLComponent, FXMLLoader> {
////        // create value so loadFXML is not deferred until the first access to the property
////        private val value: FXMLLoader = loadFXML(file)
////
////        override fun getValue(thisRef: FXMLComponent, property: KProperty<*>): FXMLLoader = value
////
////        private fun loadFXML(fxmlFile: String): FXMLLoader {
////            // we are inside anonymous object, so `this` will actually return object's this,
////            // and we need FXMLComponent's `this`
////            val loader = FXMLLoader(this@FXMLComponent::class.java.getResource(fxmlFile))
////            if (useFxmlController) {
////                loader.setControllerFactory { this@FXMLComponent }
////            } else {
////                loader.setController(this@FXMLComponent)
////            }
////            // this tells FXMLLoader, that it must use provided instance as root object
////            loader.setRoot(this@FXMLComponent)
////
////            try {
////                loader.load<Any>()
////            } catch (e: Exception) {
////
////                logger.error(e) { "Error while loading FXML `$fxmlFile`" }
////            }
////            return loader
////        }
////    }
////
////    /**
////     * Find and inject associated node from FXML to variable.
////     *
////     * You may call it Kotlin-friendly replacement for @FXML annotation.
////     *
////     * Usage:
////     * ```
////     * private val button: Button by fxid()
////     *         or
////     * private val button: Button by fxid("button")
////     * ```
////     *
////     * @param T node type
////     * @param name lookup name of the node, property name is used if null
////     */
////    fun <T : Node> fxid(name: String? = null) = object : ReadOnlyProperty<FXMLComponent, T> {
////        override fun getValue(thisRef: FXMLComponent, property: KProperty<*>): T {
////            val key = name ?: property.name
////            val value = thisRef.fxml.namespace[key]
////            @Suppress("UNCHECKED_CAST")
////            if (value == null) {
////                logger.warn { "Property $key of $thisRef was not resolved because there is no matching fx:id in ${thisRef.fxml.location}" }
////            } else {
////                return value as T
////            }
////
////            throw IllegalArgumentException("Property $key does not match fx:id declaration")
////        }
////    }
////
////
//////    inline fun <reified T : Any> di(name: String? = null): ReadOnlyProperty<FXMLComponent, T> = object : ReadOnlyProperty<FXMLComponent, T> {
//////        var injected: T? = null
//////        override fun getValue(thisRef: FXMLComponent, property: KProperty<*>): T {
//////            val dicontainer = FX.dicontainer ?: throw AssertionError(
//////                    "Injector is not configured, so bean of type ${T::class} cannot be resolved")
//////            if (injected == null) {
//////                injected = dicontainer.let {
//////                    if (name != null) {
//////                        it.getInstance<T>(name)
//////                    } else {
//////                        it.getInstance()
//////                    }
//////                }
//////            }
//////            return injected!!
//////        }
//////    }
////}
////
////inline fun <reified T : Any> FXMLComponent.di(): ReadOnlyProperty<MainWindow, T> = object : ReadOnlyProperty<MainWindow, T> {
////    override fun getValue(thisRef: MainWindow, property: KProperty<*>): T {
////        return injector.getInstance(T::class.java)
////    }
////}
