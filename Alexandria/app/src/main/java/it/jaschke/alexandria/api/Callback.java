package it.jaschke.alexandria.api;

/**
 * Callback raised when an item is selected in a fragment to access the event in activity
 * Created by saj on 25/01/15.
 */
public interface Callback {
    void onItemSelected(String ean);
}
