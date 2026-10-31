"""Check that the distributed JAR contains only the requested Ikun features and their GUI support."""
import json
import struct
import sys
import zipfile
from pathlib import Path


def constant_pool(data):
    count = struct.unpack_from('>H', data, 8)[0]
    values = {}
    classes = []
    offset = 10
    index = 1
    while index < count:
        tag = data[offset]
        offset += 1
        if tag == 1:
            size = struct.unpack_from('>H', data, offset)[0]
            offset += 2
            values[index] = data[offset:offset + size].decode('utf-8', errors='replace')
            offset += size
        elif tag == 7:
            classes.append(struct.unpack_from('>H', data, offset)[0])
            offset += 2
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            offset += 4
        elif tag in (5, 6):
            offset += 8
            index += 1
        elif tag in (8, 16, 19, 20):
            offset += 2
        elif tag == 15:
            offset += 3
        else:
            raise AssertionError(f'Unknown JVM constant tag: {tag}')
        index += 1
    return list(values.values()), [values[i] for i in classes]


def verify(path):
    assert path.stat().st_size < 1_000_000, 'Distributed mod must be below 1 MB'
    with zipfile.ZipFile(path) as jar:
        entries = set(jar.namelist())
        metadata = json.loads(jar.read('fabric.mod.json'))
        assert metadata['id'] == 'ikun' and metadata['name'] == 'ikun'
        assert metadata['entrypoints']['client'][0]['value'] == 'com.github.ikun.Ikun'
        assert metadata['depends']['minecraft'] == '26.1.2'
        assert metadata['environment'] == 'client'
        assert not metadata.get('accessWidener')
        assert metadata.get('mixins') == ['ikun.mixins.json']
        mixins = json.loads(jar.read('ikun.mixins.json'))
        assert mixins['package'] == 'com.github.ikun.mixin'
        assert mixins['client'] == ['ShopScreenMixin', 'SupplySwapMixin', 'PearlSupplyInteractMixin', 'RendRotationPacketMixin', 'RendRotationRenderMixin']
        assert not mixins.get('mixins') and not mixins.get('server')
        assert not any(name.startswith('com/github/noamm9/') for name in entries)
        assert not any(name.startswith(('com/github/iloveku/', 'assets/iloveku/')) for name in entries)
        assert 'iloveku.mixins.json' not in entries
        assert 'assets/ikun/lang/en_us.json' in entries and 'assets/ikun/lang/zh_cn.json' in entries
        assert not any(name.endswith('.jar') for name in entries), 'Unexpected bundled dependency'

        feature_classes = sorted(name for name in entries if name.startswith('com/github/ikun/features/impl/') and name.endswith('.class') and '$' not in name)
        assert feature_classes == [
            'com/github/ikun/features/impl/cheat/PearlSupplyInteract.class',
            'com/github/ikun/features/impl/cheat/RendRotate.class',
            'com/github/ikun/features/impl/general/KuudraParty.class',
            'com/github/ikun/features/impl/kuudra/AutoCannonClose.class',
            'com/github/ikun/features/impl/kuudra/AutoGFS.class',
            'com/github/ikun/features/impl/kuudra/KuudraShop.class',
            'com/github/ikun/features/impl/kuudra/SupplySwap.class',
        ], feature_classes
        assert 'com/github/ikun/ui/clickgui/ClickGuiScreen.class' in entries
        assert 'com/github/ikun/ui/clickgui/components/FeatureConfigWindow.class' in entries
        assert 'CHEAT' in constant_pool(jar.read('com/github/ikun/ui/clickgui/enums/CategoryType.class'))[0]
        config_strings = constant_pool(jar.read('com/github/ikun/config/ConfigManager.class'))[0]
        assert {'pearlSupplyInteract', 'autoGfs', 'autoGfsSettings', 'rendRotate', 'rendRotateSettings', 'clickGuiTheme'} <= set(config_strings)
        assert 'com/github/ikun/ui/clickgui/ClickGuiTheme.class' in entries
        palette_strings = constant_pool(jar.read('com/github/ikun/ui/clickgui/ClickGuiPalette.class'))[0]
        assert {'Theme Color', 'Panel Color', 'Background Color', 'Text Color', 'Muted Text Color', 'Panel Opacity', 'Background Opacity'} <= set(palette_strings)
        assert 'Colors' in constant_pool(jar.read('com/github/ikun/ui/clickgui/ClickGuiScreen.class'))[0]
        assert metadata['license'] == 'GPL-3.0-only'
        assert {'NOTICE.md', 'licenses/RatAddons-GPL-3.0.txt', 'licenses/ikun-original-CC0.txt'} <= entries
        rotation_feature = 'com/github/ikun/features/impl/cheat/RendRotate.class'
        controller = 'com/github/ikun/utils/rotation/RatHumanRotation.class'
        rotation_strings = constant_pool(jar.read(rotation_feature))[0]
        assert {'Rend Rotate', 'Rotation Delay', 'Yaw Offset Min', 'Pitch Offset Max', 'Front Min', 'Back Max'} <= set(rotation_strings)
        controller_strings = constant_pool(jar.read(controller))[0]
        for forbidden_control in ('keyUp', 'keyDown', 'keyLeft', 'keyRight', 'keyJump', 'keyShift', 'keyUse', 'keyAttack', 'setSelectedSlot', 'sendCommand', 'handleContainerInput'):
            assert forbidden_control not in rotation_strings + controller_strings, forbidden_control
        for utility in ('AutoGfsTrigger', 'PearlRefillOnEntry', 'KuudraEntryNotice', 'PearlInventory'):
            assert f'com/github/ikun/utils/kuudra/{utility}.class' in entries, utility
        assert 'com/github/ikun/utils/location/AutoGfsLocation.class' in entries
        auto_gfs_strings = constant_pool(jar.read('com/github/ikun/features/impl/kuudra/AutoGFS.class'))[0]
        assert {'AutoGFS', 'Refill Pearl', 'Toxic Amount'} <= set(auto_gfs_strings)
        toxic_strings = constant_pool(jar.read('com/github/ikun/utils/kuudra/AutoGfsTrigger.class'))[0]
        assert 'The Ballista is finally ready!' in toxic_strings
        assert any(value.startswith('gfs toxic_arrow_poison ') for value in toxic_strings)
        assert not any('shoot him with the Ballista!' in value for value in toxic_strings)
        assert 'requested' not in toxic_strings and 'lastWorld' not in toxic_strings
        party_actions = 'com/github/ikun/utils/kuudra/KuudraPartyChatActions.class'
        assert party_actions in entries
        party_action_strings = constant_pool(jar.read(party_actions))[0]
        for action in ('retry', 'kick'):
            assert any(value.startswith(f'/ikun {action} ') for value in party_action_strings), action
            assert not any(value.startswith(f'/iloveku {action} ') for value in party_action_strings), action
        command_strings = constant_pool(jar.read('com/github/ikun/Ikun.class'))[0]
        assert {'ikun', 'retry', 'kick'} <= set(command_strings), 'Missing local ikun party action commands'
        assert 'com/github/ikun/ui/kuudra/KuudraShopView.class' in entries
        shop_strings = constant_pool(jar.read('com/github/ikun/ui/kuudra/KuudraShopView.class'))[0]
        assert 'changePage' not in shop_strings and 'Confirm selection' not in shop_strings
        assert 'Original menu' not in shop_strings and 'Kuudra Shop' not in shop_strings and 'originalMenu' not in shop_strings
        editor = 'com/github/ikun/ui/kuudra/KuudraShopEditorScreen.class'
        assert editor in entries
        assert 'handleContainerInput' not in constant_pool(jar.read(editor))[0], 'HUD Edit must not purchase items'
        assert 'com/github/ikun/utils/kuudra/ShopPositions.class' in entries
        assert sorted(name for name in entries if '/mixin/' in name and name.endswith('.class')) == [
            'com/github/ikun/mixin/PearlSupplyInteractMixin.class',
            'com/github/ikun/mixin/RendRotationPacketMixin.class',
            'com/github/ikun/mixin/RendRotationRenderMixin.class',
            'com/github/ikun/mixin/ShopScreenMixin.class',
            'com/github/ikun/mixin/SupplySwapMixin.class',
        ]
        assert not any('/websocket/' in name or '/dungeons/' in name for name in entries)
        strings = []
        for name in sorted(entries):
            if not name.startswith('com/github/ikun/') or not name.endswith('.class'):
                continue
            utf8, references = constant_pool(jar.read(name))
            strings.extend(utf8)
            for reference in references:
                assert not reference.startswith(('gg/essential/', 'io/ktor/', 'me/rattokens/')), (name, reference)
                if reference.startswith('com/github/ikun/'):
                    assert reference + '.class' in entries, (name, reference)
        all_strings = '\n'.join(strings)
        for forbidden in ('api.noamm.org', 'ws.noamm.org', 'noamm.org', 'bigrat.monster', 'Query Service', '127.0.0.1:8765'):
            assert forbidden not in all_strings, forbidden
        assert 'https://newspapers-characteristics-repeat-amongst.trycloudflare.com' in all_strings
        assert not any('apiKey' in name or name.endswith('config.json') for name in entries)

        assert not metadata.get('jars'), 'Unexpected nested runtime dependencies'
        print(f'Verified {path.name}: {path.stat().st_size:,} bytes (< 1 MB), original features preserved + rotation-only Rend Rotate, saved settings and local chat commands present, five client mixins, GPL and original CC0 notices included, internal references valid, no bundled libraries.')



if __name__ == '__main__':
    verify(Path(sys.argv[1]))
