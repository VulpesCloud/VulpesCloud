package de.vulpescloud.node.services

class ServiceFactoryProvider {

    private val factories = mutableListOf<AbstractServiceFactory>()

    fun findServiceFactory(name: String): AbstractServiceFactory? {
        return factories.find { it.factoryName == name }
    }

    fun registerServiceFactory(factory: AbstractServiceFactory) {
        factories.add(factory)
    }

    fun unregisterServiceFactory(factory: AbstractServiceFactory) {
        factories.remove(factory)
    }
}
