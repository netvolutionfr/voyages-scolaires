package fr.siovision.voyages.infrastructure.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class TripRegistrationAdminViewDTOTest {

    @Test
    void listUserContractDoesNotExposeContactDetails() {
        assertThat(Arrays.stream(RegistrationListUserDTO.class.getRecordComponents())
                .map(component -> component.getName()))
                .containsExactly("publicId", "firstName", "lastName", "section")
                .doesNotContain("email", "telephone");
    }
}
