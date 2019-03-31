/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test

import cloud.orbit.core.annotation.ExecutionModel
import cloud.orbit.core.annotation.Lifecycle
import cloud.orbit.core.annotation.Routing
import cloud.orbit.core.hosting.ExecutionStrategy
import cloud.orbit.core.hosting.RandomRouting
import cloud.orbit.core.key.Key
import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.getReference
import cloud.orbit.runtime.util.StageBaseTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

@Routing(isRouted = true, persistentPlacement = false, forceRouting = false, routingStrategy = RandomRouting::class)
@Lifecycle(autoActivate = false, autoDeactivate = false)
@ExecutionModel(ExecutionStrategy.SAFE)
interface BasicAddressable : Addressable {
    fun sayHello(): Deferred<String>
}

class BasicAddressableImpl : BasicAddressable {
    override fun sayHello(): Deferred<String> {
        return CompletableDeferred("HELLO!")
    }
}

class AddressableRegistryTest : StageBaseTest() {
    @Test
    fun `ensure basic passes`() {
        val instance = BasicAddressableImpl()
        val proxy = stage.addressableRegistry
            .getReference<BasicAddressable>(Key.NoKey, NetTarget.Unicast(stage.config.nodeIdentity))

        runBlocking {
            stage.addressableRegistry.registerAddressable(BasicAddressable::class.java, Key.NoKey, instance).await()
            val result = proxy.sayHello().await()
            assertThat(result).isEqualTo("HELLO!")
            stage.addressableRegistry.deregisterAddressable(instance).await()

        }
    }

    @Test
    fun `ensure random routing passes`() {
        val instance = BasicAddressableImpl()
        val proxy = stage.addressableRegistry
            .getReference<BasicAddressable>(Key.NoKey)

        runBlocking {
            stage.addressableRegistry.registerAddressable(BasicAddressable::class.java, Key.NoKey, instance).await()
            val result = proxy.sayHello().await()
            assertThat(result).isEqualTo("HELLO!")
            stage.addressableRegistry.deregisterAddressable(instance).await()

        }
    }

    @Test
    fun `ensure fails when not registered`() {
        val proxy = stage.addressableRegistry
            .getReference<BasicAddressable>(Key.NoKey, NetTarget.Unicast(stage.config.nodeIdentity))

        assertThatThrownBy {
            runBlocking {
                proxy.sayHello().await()
            }
        }.isInstanceOf(IllegalStateException::class.java).hasMessageContaining("No active addressable")

    }

    @Test
    fun `ensure deregister removes`() {
        val instance = BasicAddressableImpl()
        val proxy = stage.addressableRegistry
            .getReference<BasicAddressable>(Key.NoKey, NetTarget.Unicast(stage.config.nodeIdentity))

        assertThatThrownBy {
            runBlocking {
                stage.addressableRegistry.registerAddressable(BasicAddressable::class.java, Key.NoKey, instance).await()
                val result = proxy.sayHello().await()
                assertThat(result).isEqualTo("HELLO!")
                stage.addressableRegistry.deregisterAddressable(instance).await()
                proxy.sayHello().await()
            }
        }.isInstanceOf(IllegalStateException::class.java).hasMessageContaining("No active addressable")
    }
}