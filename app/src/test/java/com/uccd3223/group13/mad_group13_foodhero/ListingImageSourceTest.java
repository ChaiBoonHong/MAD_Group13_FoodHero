package com.uccd3223.group13.mad_group13_foodhero;

import static org.junit.Assert.*;

import com.uccd3223.group13.mad_group13_foodhero.data.model.ImageSource;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Listing;
import com.uccd3223.group13.mad_group13_foodhero.data.remote.SupabaseConfig;
import org.junit.Test;

public class ListingImageSourceTest {

    @Test
    public void testStoragePath_resolvesFullPublicUrl() {
        String path = "merchant_123/meal_photo.jpg";
        String resolved = SupabaseConfig.getStoragePublicUrl(path);

        assertNotNull(resolved);
        assertTrue(resolved.contains("storage/v1/object/public/listing-images/"));
        assertTrue(resolved.endsWith(path));
    }

    @Test
    public void testExternalHttpsUrl_preservesOriginalUrl() {
        String externalUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c";
        String resolved = SupabaseConfig.getStoragePublicUrl(externalUrl);

        assertEquals("External URL should be preserved without double-wrapping", externalUrl, resolved);
    }

    @Test
    public void testListing_dualSourceImageEnum() {
        Listing listing1 = new Listing();
        listing1.setImageSource(ImageSource.STORAGE);
        listing1.setImageUrl(SupabaseConfig.getStoragePublicUrl("merchant_demo/bag.jpg"));

        Listing listing2 = new Listing();
        listing2.setImageSource(ImageSource.EXTERNAL_URL);
        listing2.setImageUrl("https://example.com/food.jpg");

        assertEquals(ImageSource.STORAGE, listing1.getImageSource());
        assertEquals(ImageSource.EXTERNAL_URL, listing2.getImageSource());
        assertTrue(listing1.getImageUrl().startsWith("https://"));
        assertTrue(listing2.getImageUrl().startsWith("https://"));
    }
}
