# saltttemplate

A simple template to start hytale mods from.
Update the build config stuff in settings.gradle, build.gradle and the manifset
#


example configs

loot tables

```json
{
  "Tables": [
    {
      "TableId": "Common_Loot_Items",
      "Entries": [
        { "ItemId": "Weapon_Daggers_Iron", "Quantities": [1] },
        { "ItemId": "Weapon_Arrow_Crude",        "Quantities": [10, 15, 10] } 
      ]
    },
    {
      "TableId": "Rare_Loot_Items",
      "Entries": [
        { "ItemId": "Weapon_Daggers_Iron", "Quantities": [1] },
        { "ItemId": "Weapon_Arrow_Crude",        "Quantities": [100, 150, 100] }
      ]
    }
  ]
}
```
Empty RespawnTable = chest never refills.

chest config
```json
{
  "Chests": [
    {
      "ChestItemId": "Centre_Spawn_Chest",
      "InitialSpawnTable": "Common_Loot_Items",
      "RespawnTable": "Rare_Loot_Items"
    }
  ]
}
```
