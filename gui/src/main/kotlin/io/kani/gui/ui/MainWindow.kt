package io.kani.gui.ui



class MainWindow {
    companion object


    fun onMouseClicked() {
        println("test")
//        val a = fxml("aaa")
    }
}
//
//class fxmlComponent(@Assisted fileName: String) : FxmlComponent {
//    @Inject
//    override lateinit var injector: Injector
//
//    override val fxml: FXMLLoader by fxml(fileName)
//}
//
//interface FxmlComponent {
//    val fxml: FXMLLoader
//    val injector : Injector
//    /**
//     * Helper delegate for [fxml] field initialization
//     *
//     * @param file path to .fxml resource
//     * @param useFxmlController indicates that .fxml file has set fx:controller property
//     */
//    fun fxml(file: String, useFxmlController: Boolean = true) = object : ReadOnlyProperty<FxmlComponent, FXMLLoader> {
//        // create value so loadFXML is not deferred until the first access to the property
//        private val value: FXMLLoader = loadFXML(file)
//
//        override fun getValue(thisRef: FxmlComponent, property: KProperty<*>): FXMLLoader = value
//
//        private fun loadFXML(fxmlFile: String): FXMLLoader {
//            // we are inside anonymous object, so `this` will actually return object's this,
//            // and we need FXMLComponent's `this`
//            val loader = FXMLLoader(this@FxmlComponent::class.java.getResource(fxmlFile))
////            if (useFxmlController) {
////                loader.setControllerFactory { this@FxmlComponent }
////            } else {
////                loader.setController(this@FxmlComponent)
////            }
//            // this tells FXMLLoader, that it must use provided instance as root object
//            loader.setRoot(this@FxmlComponent)
//
//            try {
//                loader.load<Any>()
//            } catch (e: Exception) {
//
//                logger.error(e) { "Error while loading FXML `$fxmlFile`" }
//            }
//            return loader
//        }
//    }
//
//    /**
//     * Find and inject associated node from FXML to variable.
//     *
//     * You may call it Kotlin-friendly replacement for @FXML annotation.
//     *
//     * Usage:
//     * ```
//     * private val button: Button by fxid()
//     *         or
//     * private val button: Button by fxid("button")
//     * ```
//     *
//     * @param T node type
//     * @param name lookup name of the node, property name is used if null
//     */
//    fun <T : Node> fxid(name: String? = null) = object : ReadOnlyProperty<FxmlComponent, T> {
//        override fun getValue(thisRef: FxmlComponent, property: KProperty<*>): T {
//            val key = name ?: property.name
//            val value = thisRef.fxml.namespace[key]
//            @Suppress("UNCHECKED_CAST")
//            if (value == null) {
//                logger.warn { "Property $key of $thisRef was not resolved because there is no matching fx:id in ${thisRef.fxml.location}" }
//            } else {
//                return value as T
//            }
//
//            throw IllegalArgumentException("Property $key does not match fx:id declaration")
//        }
//    }
//}
