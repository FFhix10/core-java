/*
 * Copyright 2019, TeamDev. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.stand;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.client.ActorRequestFactory;
import io.spine.client.Topic;
import io.spine.core.TenantId;
import io.spine.protobuf.AnyPacker;
import io.spine.server.Given.CustomerAggregate;
import io.spine.server.Given.CustomerAggregateRepository;
import io.spine.server.stand.given.StandTestEnv.MemoizeNotifySubscriptionAction;
import io.spine.test.commandservice.customer.Customer;
import io.spine.test.commandservice.customer.CustomerId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.stand.given.StandTestEnv.newStand;
import static io.spine.testing.core.given.GivenTenantId.newUuid;
import static io.spine.testing.server.entity.given.Given.aggregateOfClass;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Multitenant Stand should")
class MultitenantStandTest extends StandTest {

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
        TenantId tenantId = newUuid();

        setCurrentTenant(tenantId);
        setMultitenant(true);
        setRequestFactory(createRequestFactory(tenantId));
    }

    @AfterEach
    void tearDown() {
        clearCurrentTenant();
    }

    @SuppressWarnings("deprecation")
    // The deprecated `Stand.post()` method will become test-only in the future.
    @Test
    @DisplayName("not trigger updates of aggregate records for another tenant subscriptions")
    void updateOnlySameTenant() {
        CustomerAggregateRepository repository = new CustomerAggregateRepository();
        Stand stand = newStand(isMultitenant(), repository);

        // --- Default Tenant
        ActorRequestFactory requestFactory = getRequestFactory();
        MemoizeNotifySubscriptionAction defaultTenantCallback =
                subscribeToAllOf(stand, requestFactory, Customer.class);

        // --- Another Tenant
        TenantId anotherTenant = newUuid();
        ActorRequestFactory anotherFactory = createRequestFactory(anotherTenant);
        MemoizeNotifySubscriptionAction anotherCallback =
                subscribeToAllOf(stand, anotherFactory, Customer.class);

        // Trigger updates in Default Tenant.
        Customer customer = fillSampleCustomers(1).iterator()
                                                  .next();
        CustomerId customerId = customer.getId();
        int version = 1;
        CustomerAggregate entity = aggregateOfClass(CustomerAggregate.class)
                .withId(customerId)
                .withState(customer)
                .withVersion(version)
                .build();
        stand.post(entity, repository.lifecycleOf(customerId));

        Any packedState = AnyPacker.pack(customer);
        // Verify that Default Tenant callback has got the update.
        assertEquals(packedState, defaultTenantCallback.newEntityState());

        // And Another Tenant callback has not been called.
        assertNull(anotherCallback.newEntityState());
    }

    protected MemoizeNotifySubscriptionAction
    subscribeToAllOf(Stand stand,
                     ActorRequestFactory requestFactory,
                     Class<? extends Message> entityClass) {
        Topic allCustomers = requestFactory.topic()
                                           .allOf(entityClass);
        MemoizeNotifySubscriptionAction action = new MemoizeNotifySubscriptionAction();
        subscribeAndActivate(stand, allCustomers, action);

        assertNull(action.newEntityState());
        return action;
    }
}