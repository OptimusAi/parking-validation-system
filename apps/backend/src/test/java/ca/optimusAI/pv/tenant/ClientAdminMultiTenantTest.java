package ca.optimusAI.pv.tenant;

import ca.optimusAI.pv.shared.TenantContext;
import ca.optimusAI.pv.shared.TenantInfo;
import ca.optimusAI.pv.shared.exception.UnauthorizedTenantAccessException;
import ca.optimusAI.pv.tenant.entity.Tenant;
import ca.optimusAI.pv.tenant.repository.ClientAdminTenantRepository;
import ca.optimusAI.pv.tenant.repository.TenantRepository;
import ca.optimusAI.pv.tenant.service.AwsS3Service;
import ca.optimusAI.pv.tenant.service.TenantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verifies CLIENT_ADMIN multi-tenant scoping:
 * - Sees tenants they are assigned to
 * - Gets 403 on tenants they are NOT assigned to
 */
@ExtendWith(MockitoExtension.class)
class ClientAdminMultiTenantTest {

    @Mock TenantRepository tenantRepository;
    @Mock ClientAdminTenantRepository clientAdminTenantRepository;
    @Mock AwsS3Service awsS3Service;
    @Mock ObjectMapper objectMapper;

    @InjectMocks TenantService tenantService;

    private final UUID tenant1Id = UUID.randomUUID();
    private final UUID tenant2Id = UUID.randomUUID();
    private final UUID tenant3Id = UUID.randomUUID(); // not assigned
    private final UUID userId    = UUID.randomUUID();

    @BeforeEach
    void setClientAdminContext() {
        TenantContext.set(new TenantInfo(
                null, null, null,
                userId.toString(), "admin@test.com",
                List.of("CLIENT_ADMIN"),
                List.of(tenant1Id, tenant2Id) // only 2 tenants assigned
        ));
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void givenClientAdmin_whenListTenants_thenOnlyAssignedTenantsReturned() {
        Tenant t1 = Tenant.builder().id(tenant1Id).name("Tenant 1").build();
        Tenant t2 = Tenant.builder().id(tenant2Id).name("Tenant 2").build();
        when(tenantRepository.findAllByIdInAndIsDeletedFalse(
                argThat(ids -> ids.containsAll(List.of(tenant1Id, tenant2Id))),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(t1, t2)));

        var result = tenantService.list(0, 20);

        assertEquals(2, result.content().size());
        verify(tenantRepository).findAllByIdInAndIsDeletedFalse(any(), any());
    }

    @Test
    void givenClientAdmin_whenAccessUnassignedTenant_thenThrowUnauthorized() {
        Tenant unassignedTenant = Tenant.builder()
                .id(tenant3Id)
                .name("Other Tenant")
                .clientId(UUID.randomUUID())
                .build();
        when(tenantRepository.findByIdAndIsDeletedFalse(tenant3Id))
                .thenReturn(java.util.Optional.of(unassignedTenant));

        assertThrows(UnauthorizedTenantAccessException.class,
                () -> tenantService.get(tenant3Id));
    }

    @Test
    void givenClientAdmin_whenAccessAssignedTenant_thenSucceeds() {
        Tenant assignedTenant = Tenant.builder()
                .id(tenant1Id)
                .name("Assigned Tenant")
                .clientId(UUID.randomUUID())
                .build();
        when(tenantRepository.findByIdAndIsDeletedFalse(tenant1Id))
                .thenReturn(java.util.Optional.of(assignedTenant));

        Tenant result = tenantService.get(tenant1Id);
        assertEquals(tenant1Id, result.getId());
    }
}
