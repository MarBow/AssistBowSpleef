package to.px.marbow.AssistBowSpleef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class AssistBowSpleef extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		// 読み込まれたときに呼ばれます

		ConfigLoad();

		getLogger().info("AssistBowSpleef was loaded");
		getServer().getPluginManager().registerEvents(this, this);
	}

	// ==========================================================

	@Override
	public void onDisable() {
		// 解放されたときに呼ばれます
		getLogger().info("AssistBowSpleef was released");
	}

	// ==========================================================

	boolean flgABSEnabled = true;

	public static class ExchangeBlock {
		public Material Target = null;
		public Material Reform = null;
	}

	/** タグとアイテムと特殊動作の辞書 */
	Map<String, String> mapArea = new HashMap<String, String>();

	// ==========================================================

	private void ConfigLoad() {

		FileConfiguration fc = getConfig();
		flgABSEnabled = fc.getBoolean("Enable");

		// region登録取得
		ConfigurationSection cs = fc.getConfigurationSection("Area");
		if (cs != null) {
			Set<String> lstArea = cs.getKeys(false);// booleanは下層キーを取得するかどうかの指定

			mapArea.clear();
			for (String strArea : lstArea) {
				mapArea.put(strArea, strArea);
			}
		}
	}

	// ==========================================================

	private void ConfigSave(CommandSender sender) {

		FileConfiguration fc = getConfig();
		fc.set("Enable", flgABSEnabled);

		// region登録作成
		ConfigurationSection cs = fc.createSection("Area");

		for (String strArea : mapArea.keySet()) {
			if (cs.getConfigurationSection(strArea) != null) {
				Mes(sender, "同じ名前の登録があるのでキープしませんでした");
			} else {
				ConfigurationSection csArea = cs.createSection(strArea);
			}
		}

		saveConfig();
	}

	// ==========================================================

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		boolean flgRef = false;

		if (sender.hasPermission("AssistBowSpleef")) {
			if (command.getName().equalsIgnoreCase("abs")) {
				if (args.length > 0) {

					switch (args[0].toLowerCase()) {
					case "on":
						flgABSEnabled = true;
						Mes(sender, "AssitBowSpleefは" + ChatColor.GREEN + "有効" + ChatColor.RESET + "になりました");
						ConfigSave(sender);
						break;

					case "off":
						flgABSEnabled = false;
						Mes(sender, "AssitBowSpleefは" + ChatColor.RED + "無効" + ChatColor.RESET + "になりました");
						ConfigSave(sender);
						break;

					case "+":
						if (args.length > 1) {
							AreaAdd(sender, args[1].toLowerCase());
							ConfigSave(sender);
						} else {
							Mes(sender, "regionを指定してください");
							Mes(sender, "/abs + regionName");
						}
						flgRef = true;
						break;

					case "-":
						if (args.length > 1) {
							AreaDel(sender, args[1].toLowerCase());
							ConfigSave(sender);
						} else {
							Mes(sender, "regionを指定してください");
							Mes(sender, "/abs - regionName");
						}
						flgRef = true;
						break;

					case "list":
						ShowAllArea(sender);
						flgRef = true;
						break;
					}
				} else {
					Mes(sender, "AssitBowSpleefは" + (flgABSEnabled ? ChatColor.GREEN + "有効" : ChatColor.RED + "無効")
							+ ChatColor.RESET + "です。");
					flgRef = true;
				}
			}
		}

		return flgRef;
	}

	// ==========================================================

	void Mes(CommandSender sender, String strMes) {
		if ((sender instanceof Player)) {
			Player player = (Player) sender;
			player.sendMessage(strMes);
		} else {
			getLogger().info(strMes);
		}
	}

	// ==========================================================

	void AreaAdd(CommandSender sender, String strArea) {
		if (mapArea.containsKey(strArea)) {
			Mes(sender, strArea + "はすでに登録されています");
		} else {
			Mes(sender, strArea + "を登録しました");
			mapArea.put(strArea, strArea);
		}
	}

	// ==========================================================

	void AreaDel(CommandSender sender, String strArea) {
		if (!mapArea.containsKey(strArea)) {
			Mes(sender, strArea + "は登録されていません");
		} else {
			Mes(sender, strArea + "を登録解除しました");
			mapArea.remove(strArea);
		}
	}

	// ==========================================================

	void ShowAllArea(CommandSender sender) {
		if (mapArea.keySet().size() == 0) {
			Mes(sender, "有効領域が登録されていません");
		} else {
			Mes(sender, "有効領域:" + String.valueOf(mapArea.keySet().size()));
			for (String strArea : mapArea.keySet()) {
				Mes(sender, "  " + strArea);
			}
		}
	}

	// ==========================================================

	@EventHandler(priority = EventPriority.LOWEST)
	public boolean onProjectileHit(ProjectileHitEvent sender) {
		boolean flgRef = false;

		// getLogger().info("HitAny");

		Entity arrow = sender.getEntity();
		Block block = sender.getHitBlock();

		boolean flgEnabledArea = false;

		if (getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
			WorldGuardPlugin worldGuard = (WorldGuardPlugin) getServer().getPluginManager().getPlugin("WorldGuard");
			RegionContainer region = worldGuard.getRegionContainer();
			if (region != null) {
				ApplicableRegionSet ars = region.get(arrow.getWorld()).getApplicableRegions(arrow.getLocation());
				if (ars != null && ars.size() > 0) {
					for (ProtectedRegion lRegion : ars) {
						getLogger().info(lRegion.getId());
						flgEnabledArea = true;
					}
				}
			}
		}

		if (flgEnabledArea) {
			if (arrow.getPassengers().size() > 0) {
				getLogger().info(arrow.getPassengers().get(0).getName());
			}

			if (block != null) {
				getLogger().info(arrow.getName() + " hit " + block.getType().name());

				if (block.getType() == Material.TNT) {
					block.setType(Material.AIR);
					block.getWorld().spawnEntity(block.getLocation(), EntityType.PRIMED_TNT);
					arrow.remove();
				}
			} else {
				Entity entity = sender.getHitEntity();
				// getLogger().info(arrow.getName() + " hit " +
				// entity.getName());
			}
		}

		return flgRef;
	}
}
