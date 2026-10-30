package com.example.anvilcostfix;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.java.JavaPlugin;

public class AnvilCostFix extends JavaPlugin implements Listener {

    private boolean resetResultPenalty;
    private boolean ignoreExistingPenalty;
    private boolean removeTooExpensive;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadSettings() {
        reloadConfig();
        resetResultPenalty = getConfig().getBoolean("reset-result-penalty", true);
        ignoreExistingPenalty = getConfig().getBoolean("ignore-existing-penalty", true);
        removeTooExpensive = getConfig().getBoolean("remove-too-expensive", true);
    }

    /** 모루를 열 때 최대 비용 제한을 먼저 풀어 둔다 (첫 계산부터 '너무 비쌈'이 안 뜨게). */
    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (removeTooExpensive && event.getView() instanceof AnvilView view) {
            view.setMaximumRepairCost(Integer.MAX_VALUE);
        }
    }

    @EventHandler
    public void onPrepare(PrepareAnvilEvent event) {
        AnvilView view = event.getView();
        AnvilInventory inv = event.getInventory();

        if (removeTooExpensive) {
            view.setMaximumRepairCost(Integer.MAX_VALUE);
        }

        ItemStack result = event.getResult();
        if (result == null || result.getType().isAir()) return;

        // 1) 결과물에 패널티가 쌓이지 않게 0으로 초기화
        if (resetResultPenalty) {
            ItemMeta meta = result.getItemMeta();
            if (meta instanceof Repairable repairable) {
                repairable.setRepairCost(0);
                result.setItemMeta(meta);
                event.setResult(result);
            }
        }

        // 2) 재료에 이미 쌓인 패널티를 비용에서 제외
        if (ignoreExistingPenalty) {
            int penalty = penaltyOf(inv.getFirstItem()) + penaltyOf(inv.getSecondItem());
            if (penalty > 0) {
                view.setRepairCost(Math.max(1, view.getRepairCost() - penalty));
            }
        }
    }

    private static int penaltyOf(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;
        ItemMeta meta = item.getItemMeta();
        return meta instanceof Repairable r ? r.getRepairCost() : 0;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            loadSettings();
            sender.sendMessage("AnvilCostFix 설정을 다시 불러왔습니다.");
            return true;
        }
        return false;
    }
}
