import os
import re

EXTRAS = {
    'en_US': {
        'deny-pvp': '"&cPvP is disabled in this claimed chunk."',
        'gui-flag-greeting-title': '"&eShow Welcome Title"',
        'greeting-subtitle': '"&7Owned by %player%"',
        'gui-item-rename': '"&eRename Claim"',
        'rename-prompt': '"&aPlease type the new name for &e%name% &ain chat, or type \'cancel\' to abort."',
        'rename-success': '"&aSuccessfully renamed claim to &e%name%&a!"',
        'rename-cancel': '"&cRenaming cancelled."',
        'gui-flag-interact-chest': '"&eProtect Chests & Containers"',
        'gui-flag-interact-furnace': '"&eProtect Furnaces"',
        'gui-flag-interact-stonecutter': '"&eProtect Crafting/Utilities"',
        'gui-flag-interact-door': '"&eProtect Doors/Trapdoors"',
        'gui-flag-interact-redstone': '"&eProtect Redstone Inputs"'
    },
    'es_ES': {
        'gui-flag-greeting-title': '"&eMostrar Título de Bienvenida"',
        'greeting-subtitle': '"&7Propiedad de %player%"',
        'gui-item-rename': '"&eRenombrar Reclamo"',
        'rename-prompt': '"&aPor favor, escribe el nuevo nombre para &e%name% &aen el chat, o escribe \'cancelar\' para abortar."',
        'rename-success': '"&a¡Reclamo renombrado a &e%name% &acon éxito!"',
        'rename-cancel': '"&cRenombramiento cancelado."',
        'gui-flag-interact-chest': '"&eProteger Cofres y Contenedores"',
        'gui-flag-interact-furnace': '"&eProteger Hornos"',
        'gui-flag-interact-stonecutter': '"&eProteger Cortapiedras/Utilidades"',
        'gui-flag-interact-door': '"&eProteger Puertas/Trampillas"',
        'gui-flag-interact-redstone': '"&eProteger Entradas de Redstone"'
    },
    'fr_FR': {
        'gui-flag-greeting-title': '"&eAfficher le Message de Bienvenue"',
        'greeting-subtitle': '"&7Appartient à %player%"',
        'gui-item-rename': '"&eRenommer la Zone"',
        'rename-prompt': '"&aVeuillez écrire le nouveau nom pour &e%name% &adans le chat, ou \'annuler\' pour abandonner."',
        'rename-success': '"&aLa zone a été renommée en &e%name% &aavec succès !"',
        'rename-cancel': '"&cRenommage annulé."',
        'gui-flag-interact-chest': '"&eProtéger Coffres & Conteneurs"',
        'gui-flag-interact-furnace': '"&eProtéger Fours"',
        'gui-flag-interact-stonecutter': '"&eProtéger Outils/Ateliers"',
        'gui-flag-interact-door': '"&eProtéger Portes/Trappes"',
        'gui-flag-interact-redstone': '"&eProtéger les Entrées Redstone"'
    },
    'zh_CN': {
        'gui-flag-greeting-title': '"&e显示欢迎标题"',
        'greeting-subtitle': '"&7所有者: %player%"',
        'gui-item-rename': '"&e重命名领地"',
        'rename-prompt': '"&a请在聊天中为 &e%name% &a输入新名称，或者输入 \'取消\' 以放弃。"',
        'rename-success': '"&a成功将领地重命名为 &e%name%&a！"',
        'rename-cancel': '"&c重命名已取消。"',
        'gui-flag-interact-chest': '"&e保护 箱子/容器"',
        'gui-flag-interact-furnace': '"&e保护 熔炉"',
        'gui-flag-interact-stonecutter': '"&e保护 工作台/实用方块"',
        'gui-flag-interact-door': '"&e保护 门/活板门"',
        'gui-flag-interact-redstone': '"&e保护 红石输入"'
    }
}

for lang, extra in EXTRAS.items():
    file_path = f'src/main/resources/lang/messages_{lang}.yml'
    if not os.path.exists(file_path): continue
    
    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
        text = f.read()
    
    # Process text into dictionary-like structure to detect existing vs missing keys
    # First, let's clean any double-merged lines like: deny-pvp: "..."gui-flag-...
    text = re.sub(r'("\S)(gui-flag-|greeting|rename)', r'"\n\2', text)
    
    lines = [L.strip() for L in text.split('\n') if L.strip()]
    existing_keys = set()
    cleaned_lines = []
    
    for l in lines:
        if ':' in l:
            k = l.split(':', 1)[0].strip()
            existing_keys.add(k)
            # Remove bad formatting if it exists
            l = l.replace(r'\"\&', '"&')
            l = l.replace(r'\"&', '"&')
            l = l.replace(r'\&', '&')
            
            # Remove from existing if it's one of our extras we want to strictly overwrite
            if k in extra:
                pass # Wait, if it exists, let's just overwrite it later
            else:
                cleaned_lines.append(l)
        else:
            cleaned_lines.append(l)
    
    for k, v in extra.items():
        cleaned_lines.append(f"{k}: {v}")
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(cleaned_lines) + '\n')
    
print("Updated en_US, es_ES, fr_FR, and zh_CN.")
