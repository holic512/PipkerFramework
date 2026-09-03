package com.pipker.starter.common.machine;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MachineInfoUtilsTests {

    @Test
    void readsAnImmutableBestEffortMachineSnapshot() {
        MachineInfo machineInfo = MachineInfoUtils.getLocalMachineInfo();

        assertThat(machineInfo.operatingSystemName()).isNotBlank();
        assertThat(machineInfo.operatingSystemVersion()).isNotBlank();
        assertThat(machineInfo.architecture()).isNotBlank();
        assertThat(machineInfo.availableProcessors()).isPositive();
        assertThat(machineInfo.jvmName()).isNotBlank();
        assertThat(machineInfo.jvmVersion()).isNotBlank();
        assertThatThrownBy(() -> machineInfo.networkInterfaces().add(null))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(machineInfo.networkInterfaces()).allSatisfy(networkInterface -> {
            assertThat(networkInterface.name()).isNotBlank();
            assertThat(networkInterface.macAddress())
                    .matches("(?:[0-9A-F]{2}(?::[0-9A-F]{2})*)?");
            assertThat(networkInterface.ipAddresses()).allSatisfy(address ->
                    assertThatCode(() -> InetAddress.getByName(address)).doesNotThrowAnyException()
            );
        });
    }
}
