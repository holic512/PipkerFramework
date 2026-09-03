package com.pipker.starter.common.util;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class IdentifierUtilsTests {

    @Test
    void generatesStandardAndCompactRandomUuids() {
        String uuid = UuidUtils.randomUuid();
        String compactUuid = UuidUtils.randomUuidWithoutHyphens();

        assertThat(uuid).matches("[a-f0-9]{8}-(?:[a-f0-9]{4}-){3}[a-f0-9]{12}");
        assertThat(UUID.fromString(uuid).toString()).isEqualTo(uuid);
        assertThat(compactUuid).matches("[a-f0-9]{32}");
    }

    @Test
    void generatesBase62IdentifiersAtDefaultAndRequestedLengths() {
        assertThat(RandomIdentifierUtils.randomAlphanumeric()).matches("[A-Za-z0-9]{32}");
        assertThat(RandomIdentifierUtils.randomAlphanumeric(48)).matches("[A-Za-z0-9]{48}");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> RandomIdentifierUtils.randomAlphanumeric(0))
                .withMessage("length must be greater than zero");
    }

    @Test
    void generatesDistinctIdentifiersAcrossRepeatedCalls() {
        Set<String> identifiers = IntStream.range(0, 256)
                .mapToObj(index -> RandomIdentifierUtils.randomAlphanumeric())
                .collect(Collectors.toSet());

        assertThat(identifiers).hasSize(256);
    }
}
