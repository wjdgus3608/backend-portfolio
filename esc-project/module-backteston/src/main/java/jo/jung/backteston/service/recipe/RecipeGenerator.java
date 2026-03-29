package jo.jung.backteston.service.recipe;

import jo.jung.backteston.service.algorithm.AlgorithmGenerator;
import jo.jung.backteston.service.algorithm.BuyAlgorithm;
import jo.jung.backteston.service.algorithm.SellAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RecipeGenerator {

    @Autowired
    private AlgorithmGenerator algorithmGenerator;

    public List<Recipe> generateAllRecipes() {
        List<BuyAlgorithm> buyAlgos = algorithmGenerator.generateAllBuyAlgorithms();
        List<SellAlgorithm> sellAlgos = algorithmGenerator.generateAllSellAlgorithms();

        List<Recipe> recipes = new ArrayList<>();
        for (BuyAlgorithm buy : buyAlgos) {
            for (SellAlgorithm sell : sellAlgos) {
                recipes.add(new Recipe(buy, sell));
            }
        }

        return recipes;
    }
}
