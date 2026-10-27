import { ActivityIndicator, Pressable, StyleSheet, View, type PressableProps } from 'react-native';
import { Text } from './Text';
import { useTheme } from '@/theme/themeContext';

type ButtonProps = Omit<PressableProps, 'children'> & {
  label: string;
  loading?: boolean;
};

/** Primary button from the auth designs: sage fill, caps label, hugs its content. */
export function Button({ label, loading, disabled, style, ...rest }: ButtonProps) {
  const { colors } = useTheme();
  const inactive = disabled || loading;

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled: !!inactive, busy: !!loading }}
      disabled={inactive}
      hitSlop={8}
      style={(state) => [
        styles.button,
        { backgroundColor: colors.primary, opacity: disabled ? 0.5 : state.pressed ? 0.85 : 1 },
        typeof style === 'function' ? style(state) : style,
      ]}
      {...rest}
    >
      {/* Keep the label in the layout while loading so the button doesn't change width. */}
      <View style={{ opacity: loading ? 0 : 1 }}>
        <Text variant="button" color="onPrimary">
          {label.toUpperCase()}
        </Text>
      </View>
      {loading && <ActivityIndicator color={colors.onPrimary} size="small" style={StyleSheet.absoluteFill} />}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    alignSelf: 'center',
    height: 30,
    paddingHorizontal: 18,
    borderRadius: 4,
    alignItems: 'center',
    justifyContent: 'center',
    boxShadow: '0 1px 3px rgba(0, 0, 0, 0.25)',
  },
});
