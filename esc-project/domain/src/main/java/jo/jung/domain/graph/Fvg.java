package jo.jung.domain.graph;

import lombok.*;

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Fvg {
    public long top;
    public long bottom;
    public int createdIndex;
    public boolean filled;
}
