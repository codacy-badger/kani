//package io.kani.di
//
//import com.google.inject.*
//import com.google.inject.spi.TypeConverterBinding
//import javafx.application.Application
//import javafx.fxml.FXMLLoader
//
///**
// * Wrapper around [Injector] with automatic JavaFX module injection
// */
//class FXInjector(val app: Application, modules: Set<Module>) : Injector {
//    private inner class FXModule : AbstractModule() {
//        // Any injection of [Application] will end up providing this instance
//        @Provides
//        internal fun provideApplication(): Application = app
//
//        @Provides
//        internal fun provideFxmlLoader() = FXMLLoader().apply {
//            // when factory is present, for any mentioned controller inside FXML
//            // loader will invoke this callback
//            setControllerFactory {
//                // by doing this we delegate controller creation to DI framework
//                this@FXInjector.getInstance(it)
//            }
//        }
//    }
//
//    private val delegate: Injector = Guice.createInjector(modules + FXModule())
//
//    init {
//        // start injection process
//        delegate.injectMembers(app)
//    }
//
//    override fun getAllBindings(): MutableMap<Key<*>, Binding<*>> = delegate.allBindings
//    override fun injectMembers(instance: Any) = delegate.injectMembers(instance)
//    override fun getParent(): Injector = delegate.parent
//    override fun <T : Any> getExistingBinding(key: Key<T>): Binding<T> = delegate.getExistingBinding(key)
//    override fun <T : Any> getMembersInjector(typeLiteral: TypeLiteral<T>): MembersInjector<T> = delegate.getMembersInjector(typeLiteral)
//    override fun <T : Any> getMembersInjector(type: Class<T>): MembersInjector<T> = delegate.getMembersInjector(type)
//    override fun <T : Any> findBindingsByType(type: TypeLiteral<T>): MutableList<Binding<T>> = delegate.findBindingsByType(type)
//    override fun getTypeConverterBindings(): MutableSet<TypeConverterBinding> = delegate.typeConverterBindings
//    override fun <T : Any> getBinding(key: Key<T>): Binding<T> = delegate.getBinding(key)
//    override fun <T : Any> getBinding(type: Class<T>): Binding<T> = delegate.getBinding(type)
//    override fun <T : Any> getProvider(key: Key<T>): Provider<T> = delegate.getProvider(key)
//    override fun <T : Any> getProvider(type: Class<T>): Provider<T> = delegate.getProvider(type)
//    override fun getBindings(): MutableMap<Key<*>, Binding<*>> = delegate.bindings
//    override fun getScopeBindings(): MutableMap<Class<out Annotation>, Scope> = delegate.scopeBindings
//    override fun createChildInjector(modules: Iterable<Module>): Injector = delegate.createChildInjector(modules)
//    override fun createChildInjector(vararg modules: Module): Injector = delegate.createChildInjector(modules.toList())
//    override fun <T : Any> getInstance(key: Key<T>): T = delegate.getInstance(key)
//    override fun <T : Any> getInstance(type: Class<T>): T = delegate.getInstance(type)
//}
