package skinsrestorer.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import com.onarandombox.MultiverseCore.MultiverseCore;

public class MCoreAPI {

	private static MultiverseCore mcore = null;
	
	public static void init() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
 
        if (plugin instanceof MultiverseCore) {
            mcore = (MultiverseCore) plugin;
        }
    }
	public static double getWorldScale(World world){
		return mcore.getMVWorldManager().getMVWorld(world).getScaling();
	}
	
	public static boolean check(){
		if (mcore!=null){
			return true;
		}
		return false;
	}
	public static int dimension(World world){
		if (getWorldScale(world)==1){
		   return 0;
		}if (getWorldScale(world)==8){
			return -1;
		}if (getWorldScale(world)==16){
			return 1;
		}
		return 0;
	}
}
