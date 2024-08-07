package gregtech.api.util;

import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.recipes.Recipe;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.Iterator;
import java.util.LinkedList;

public class RecipeLRUCache {
    private final int capacity;
    private Recipe lastAccessedRecipe;
    private final LinkedList<Recipe> recipeCaches;

    private int cacheHit = 0;
    private int cacheMiss = 0;

    public RecipeLRUCache(int capacity) {
        this.capacity = capacity;
        this.recipeCaches = new LinkedList<>();
    }

    public int getCachedRecipeCount() {
        return this.recipeCaches.size();
    }

    public int getCacheHit() {
        return this.cacheHit;
    }

    public int getCacheMiss() {
        return this.cacheMiss;
    }

    public Recipe get(IItemHandlerModifiable inputItems, IMultipleTankHandler inputFluids) {
        for (Recipe recipeCache : this.recipeCaches) {
            boolean foundMatches = recipeCache.matches(false, inputItems, inputFluids);
            if (foundMatches) {
                this.lastAccessedRecipe = recipeCache;
                return recipeCache;
            }
        }
        return null;
    }


    public Recipe getReverse(IItemHandlerModifiable inputItems, IMultipleTankHandler inputFluids) {
        Iterator<Recipe> recipeCachesIterator = this.recipeCaches.descendingIterator();
        while (recipeCachesIterator.hasNext()) {
            Recipe recipeCache = recipeCachesIterator.next();
            boolean foundMatches = recipeCache.matches(false, inputItems, inputFluids);
            if (foundMatches) {
                this.lastAccessedRecipe = recipeCache;
                return recipeCache;
            }
        }
        return null;
    }

    public int cacheUtilized() {
        this.recipeCaches.remove(this.lastAccessedRecipe);
        this.recipeCaches.addFirst(this.lastAccessedRecipe);
        this.cacheHit++;
        return this.cacheHit;
    }

    public int cacheUnutilized() {
        this.cacheMiss++;
        return this.cacheMiss;
    }

    public void put(Recipe value) {
        if (capacity <= 0) {
            return;
        }

        if (this.recipeCaches.size() >= this.capacity) {
            this.recipeCaches.removeLast();
        }

        this.recipeCaches.addFirst(value);
    }
}
