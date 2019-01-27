//package io.kani.di
//
//import com.google.inject.Injector
//import kotlin.properties.ReadOnlyProperty
//import kotlin.reflect.KProperty
//
//interface DiInjectable {
//    val injector: Injector
//}
//
//inline fun <reified T : Any> DiInjectable.di(name: String? = null) = object : ReadOnlyProperty<DiInjectable, T> {
//    override fun getValue(thisRef: DiInjectable, property: KProperty<*>): T {
//        return injector.getInstance(T::class.java)
//    }
//}
