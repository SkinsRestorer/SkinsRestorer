package skinsrestorer.shared.api;

import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.SkinFetchUtils;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class SkinsRestorerAPI {
	   
	/**
	 * This method is used to set player's skin.
	 * <p>
	 */
	   public static void setSkin(final String playerName, final String skinName) throws SkinFetchFailedException{
			SkinProfile skinprofile = SkinFetchUtils.fetchSkinProfile(skinName, null);
			SkinStorage.getInstance().setSkinData(playerName, skinprofile);
	   }
	   
		/**
		 * This method is used to check if player has
		 * saved skin data. If player skin data equals null, the method will return false.
		 * Else if player has saved data, it will return true.
		 */
       public static boolean hasSkin(String playerName){
    	   if (SkinStorage.getInstance().getSkinData(playerName)==null){
    		   return false;
    	   }
    	   return true;
       }
       
       /**
        * This method is used to get player's skin name.
        * If player doesn't have skin, the method will return null.
        * Else it will return player's skin name.
        */
       public static String getSkinName(String playerName){
    	   SkinProfile data = SkinStorage.getInstance().getSkinData(playerName);
    	   if (data==null){
    		   return null;
    	   }
    	   return data.getName();
       }
}
