package jo.jung.backteston.service.recipe;

import jo.jung.backteston.service.algorithm.BuyAlgorithm;
import jo.jung.backteston.service.algorithm.SellAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Recipe {
    private BuyAlgorithm buyAlgo;
    private SellAlgorithm sellAlgo;
}
