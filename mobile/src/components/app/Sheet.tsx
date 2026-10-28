import type { ReactNode } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Text } from '@/components/Text';
import { Icon } from '@/theme/icons';
import { fontFamily } from '@/theme/fonts';
import { useTheme } from '@/theme/themeContext';

/** Bottom sheet over a dimmed backdrop. Tapping the backdrop closes it. */
export function Sheet({ visible, onClose, title, children }: { visible: boolean; onClose: () => void; title: string; children: ReactNode }) {
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();
  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose} statusBarTranslucent>
      <View style={styles.wrap}>
        <Pressable style={StyleSheet.absoluteFill} onPress={onClose} accessibilityLabel="Close" accessibilityRole="button">
          <View style={[StyleSheet.absoluteFill, { backgroundColor: 'rgba(0,0,0,0.35)' }]} />
        </Pressable>
        <View style={[styles.sheet, { backgroundColor: colors.background, paddingBottom: insets.bottom + 16 }]} accessibilityViewIsModal>
          <View style={styles.handle} />
          <View style={styles.head}>
            <Text variant="h3" color="selected" accessibilityRole="header" style={{ flex: 1, fontFamily: fontFamily('ui', 600) }}>
              {title}
            </Text>
            <Pressable onPress={onClose} hitSlop={12} accessibilityRole="button" accessibilityLabel="Close">
              <Icon.close size={24} color={colors.text} />
            </Pressable>
          </View>
          <ScrollView contentContainerStyle={{ paddingHorizontal: 20, paddingBottom: 8 }}>{children}</ScrollView>
        </View>
      </View>
    </Modal>
  );
}

/** Two-button confirm inside a Sheet. */
export function ConfirmSheet({
  visible,
  title,
  body,
  confirm,
  onConfirm,
  onClose,
  danger,
}: {
  visible: boolean;
  title: string;
  body: string;
  confirm: string;
  onConfirm: () => void;
  onClose: () => void;
  danger?: boolean;
}) {
  const { colors } = useTheme();
  return (
    <Sheet visible={visible} onClose={onClose} title={title}>
      <Text variant="body" color="text">
        {body}
      </Text>
      <View style={{ flexDirection: 'row', justifyContent: 'flex-end', gap: 22, marginTop: 24, alignItems: 'center' }}>
        <Text variant="buttonLarge" color="subtle" onPress={onClose} accessibilityRole="button">
          CANCEL
        </Text>
        <Pressable
          onPress={onConfirm}
          accessibilityRole="button"
          style={[styles.confirm, { backgroundColor: danger ? colors.danger : colors.primary }]}
        >
          <Text variant="buttonLarge" style={{ color: danger ? '#FFFFFF' : colors.onPrimary }}>
            {confirm.toUpperCase()}
          </Text>
        </Pressable>
      </View>
    </Sheet>
  );
}

const styles = StyleSheet.create({
  wrap: { flex: 1, justifyContent: 'flex-end' },
  sheet: { maxHeight: '85%', borderTopLeftRadius: 20, borderTopRightRadius: 20, paddingTop: 8 },
  handle: { alignSelf: 'center', width: 40, height: 4, borderRadius: 2, backgroundColor: 'rgba(0,0,0,0.15)', marginBottom: 6 },
  head: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 20, paddingVertical: 10 },
  confirm: { height: 36, paddingHorizontal: 18, borderRadius: 4, alignItems: 'center', justifyContent: 'center' },
});
