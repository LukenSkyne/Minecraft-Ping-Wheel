package nx.pingwheel.client.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.util.Identifier;

import static nx.pingwheel.shared.PingWheel.MOD_ID;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Icon {
    Identifier textureId;
    int index;

    public Icon(int index) {
        this.index = index;
        updateTextureId();
    }

    public void nextIcon(int max) {
        this.index = (this.index + 1) % max;
        updateTextureId();
    }

    private void updateTextureId() {
        this.textureId =new Identifier(MOD_ID, String.format("textures/ping/icon_%s.png", this.index));
    }
}
