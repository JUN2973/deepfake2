package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ImdHeatmapClientPaletteTest {

    private final ImdHeatmapClient client = new ImdHeatmapClient(
            RestClient.create(),
            new ObjectMapper(),
            mock(ImdLocalServiceManager.class)
    );

    @Test
    void usesMagentaAndRedInsteadOfYellow() {
        assertThat(client.highlightColor(0.5)).containsExactly(255, 45, 149);
        assertThat(client.highlightColor(1.0)).containsExactly(255, 23, 68);
        assertThat(client.highlightColor(1.0)[1]).isLessThan(60);
    }

    @Test
    void usesCyanOutlineForContrast() {
        Color outline = ImdHeatmapClient.HEATMAP_OUTLINE_COLOR;

        assertThat(outline.getRed()).isZero();
        assertThat(outline.getGreen()).isEqualTo(229);
        assertThat(outline.getBlue()).isEqualTo(255);
        assertThat(outline.getAlpha()).isEqualTo(245);
    }
}
