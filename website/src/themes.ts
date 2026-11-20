export type PaletteId =
  | 'OCEAN_DAWN'
  | 'EMERALD_FOREST'
  | 'SUNSET_CORAL'
  | 'ROYAL_AMETHYST'
  | 'MIDNIGHT_SLATE'
  | 'CRIMSON_PULSE';

export interface PaletteTokens {
  bg: string;
  surface: string;
  surfaceLow: string;
  surfaceHigh: string;
  surfaceHighest: string;
  primary: string;
  onPrimary: string;
  primaryContainer: string;
  onPrimaryContainer: string;
  secondary: string;
  secondaryContainer: string;
  tertiary: string;
  tertiaryContainer: string;
  onSurface: string;
  onSurfaceVariant: string;
  outline: string;
  outlineVariant: string;
}

export interface TimecurePalette {
  id: PaletteId;
  name: string;
  tagline: string;
  preview: [string, string, string];
  dark: PaletteTokens;
  light: PaletteTokens;
}

export const TIMECURE_PALETTES: TimecurePalette[] = [
  {
    id: 'OCEAN_DAWN',
    name: 'Ocean Dawn',
    tagline: 'Crisp blue & warm amber',
    preview: ['#0061A4', '#535F70', '#8B5000'],
    dark: {
      bg: '#0C1219',
      surface: '#111821',
      surfaceLow: '#161F2A',
      surfaceHigh: '#1F2A38',
      surfaceHighest: '#2A3646',
      primary: '#9ECAFF',
      onPrimary: '#003258',
      primaryContainer: '#00497D',
      onPrimaryContainer: '#D1E4FF',
      secondary: '#BBC7DB',
      secondaryContainer: '#3B4858',
      tertiary: '#FFB95C',
      tertiaryContainer: '#693C00',
      onSurface: '#E2E8F0',
      onSurfaceVariant: '#94A3B8',
      outline: '#475569',
      outlineVariant: '#263242',
    },
    light: {
      bg: '#F7FAFE',
      surface: '#FFFFFF',
      surfaceLow: '#EFF4FA',
      surfaceHigh: '#E4ECF5',
      surfaceHighest: '#D9E4F0',
      primary: '#0061A4',
      onPrimary: '#FFFFFF',
      primaryContainer: '#D1E4FF',
      onPrimaryContainer: '#001D36',
      secondary: '#535F70',
      secondaryContainer: '#D7E3F7',
      tertiary: '#8B5000',
      tertiaryContainer: '#FFDCBA',
      onSurface: '#111827',
      onSurfaceVariant: '#4B5563',
      outline: '#73777F',
      outlineVariant: '#CBD5E1',
    },
  },
  {
    id: 'EMERALD_FOREST',
    name: 'Emerald',
    tagline: 'Botanical green & gold',
    preview: ['#006B54', '#4B635B', '#8A5A00'],
    dark: {
      bg: '#0A120F',
      surface: '#0E1714',
      surfaceLow: '#14201C',
      surfaceHigh: '#1D2C27',
      surfaceHighest: '#273832',
      primary: '#6ADBB8',
      onPrimary: '#00382B',
      primaryContainer: '#00513F',
      onPrimaryContainer: '#88F8D3',
      secondary: '#B3CCC2',
      secondaryContainer: '#344C44',
      tertiary: '#F6BE48',
      tertiaryContainer: '#634000',
      onSurface: '#E2EFEA',
      onSurfaceVariant: '#91ADA3',
      outline: '#445C54',
      outlineVariant: '#22332D',
    },
    light: {
      bg: '#F4FBF8',
      surface: '#FFFFFF',
      surfaceLow: '#ECF5F1',
      surfaceHigh: '#E0ECE7',
      surfaceHighest: '#D4E4DD',
      primary: '#006B54',
      onPrimary: '#FFFFFF',
      primaryContainer: '#88F8D3',
      onPrimaryContainer: '#002018',
      secondary: '#4B635B',
      secondaryContainer: '#CFE9DE',
      tertiary: '#845400',
      tertiaryContainer: '#FFDDB2',
      onSurface: '#0F1F1A',
      onSurfaceVariant: '#445B53',
      outline: '#6B7E77',
      outlineVariant: '#C6DBD2',
    },
  },
  {
    id: 'SUNSET_CORAL',
    name: 'Sunset Coral',
    tagline: 'Energetic coral & warm clay',
    preview: ['#B33B15', '#77574D', '#6B5E2F'],
    dark: {
      bg: '#160F0D',
      surface: '#1D1412',
      surfaceLow: '#251B18',
      surfaceHigh: '#322521',
      surfaceHighest: '#3E302C',
      primary: '#FFB59D',
      onPrimary: '#5F1600',
      primaryContainer: '#872506',
      onPrimaryContainer: '#FFDBD0',
      secondary: '#E7BDB0',
      secondaryContainer: '#5D4036',
      tertiary: '#D6C78E',
      tertiaryContainer: '#51471A',
      onSurface: '#F4E6E2',
      onSurfaceVariant: '#B89D94',
      outline: '#6B524A',
      outlineVariant: '#3B2B26',
    },
    light: {
      bg: '#FFF8F6',
      surface: '#FFFFFF',
      surfaceLow: '#FDF0EC',
      surfaceHigh: '#F7E5DF',
      surfaceHighest: '#EED9D2',
      primary: '#B33B15',
      onPrimary: '#FFFFFF',
      primaryContainer: '#FFDBD0',
      onPrimaryContainer: '#3B0B00',
      secondary: '#77574D',
      secondaryContainer: '#FFDBD0',
      tertiary: '#6A5E2F',
      tertiaryContainer: '#F3E3A8',
      onSurface: '#231612',
      onSurfaceVariant: '#614840',
      outline: '#856B62',
      outlineVariant: '#E0C7BE',
    },
  },
  {
    id: 'ROYAL_AMETHYST',
    name: 'Amethyst',
    tagline: 'Deep violet & teal accent',
    preview: ['#6342C9', '#5F5B71', '#00696E'],
    dark: {
      bg: '#0F0E16',
      surface: '#15141F',
      surfaceLow: '#1C1B28',
      surfaceHigh: '#272536',
      surfaceHighest: '#323044',
      primary: '#CBBFFF',
      onPrimary: '#330099',
      primaryContainer: '#4B27B2',
      onPrimaryContainer: '#E7DEFF',
      secondary: '#C9C3DC',
      secondaryContainer: '#484459',
      tertiary: '#7FD5D8',
      tertiaryContainer: '#004F53',
      onSurface: '#EAE8F5',
      onSurfaceVariant: '#A19CB8',
      outline: '#57526B',
      outlineVariant: '#2E2B3E',
    },
    light: {
      bg: '#FCF8FF',
      surface: '#FFFFFF',
      surfaceLow: '#F4EFFA',
      surfaceHigh: '#EBE4F5',
      surfaceHighest: '#E1D9EE',
      primary: '#6342C9',
      onPrimary: '#FFFFFF',
      primaryContainer: '#E7DEFF',
      onPrimaryContainer: '#1E0060',
      secondary: '#5F5B71',
      secondaryContainer: '#E5DFF9',
      tertiary: '#00696E',
      tertiaryContainer: '#9CF1F4',
      onSurface: '#1A1626',
      onSurfaceVariant: '#534E66',
      outline: '#7A748E',
      outlineVariant: '#D5CEE6',
    },
  },
  {
    id: 'MIDNIGHT_SLATE',
    name: 'Graphite',
    tagline: 'Minimal slate & cobalt',
    preview: ['#334E68', '#52606D', '#00639A'],
    dark: {
      bg: '#0B0F13',
      surface: '#11161C',
      surfaceLow: '#171D24',
      surfaceHigh: '#212A33',
      surfaceHighest: '#2C3641',
      primary: '#ACC7E3',
      onPrimary: '#14314B',
      primaryContainer: '#2B4863',
      onPrimaryContainer: '#D5E7FA',
      secondary: '#BCC7D2',
      secondaryContainer: '#3C4852',
      tertiary: '#8CCEFF',
      tertiaryContainer: '#004B6F',
      onSurface: '#E5ECF2',
      onSurfaceVariant: '#93A2B0',
      outline: '#4D5C6A',
      outlineVariant: '#26313C',
    },
    light: {
      bg: '#F6F9FC',
      surface: '#FFFFFF',
      surfaceLow: '#EDF2F6',
      surfaceHigh: '#E3EAF0',
      surfaceHighest: '#D8E1E9',
      primary: '#334E68',
      onPrimary: '#FFFFFF',
      primaryContainer: '#D5E7FA',
      onPrimaryContainer: '#0A2033',
      secondary: '#52606D',
      secondaryContainer: '#D8E3EE',
      tertiary: '#00639A',
      tertiaryContainer: '#CAE6FF',
      onSurface: '#101820',
      onSurfaceVariant: '#4A5866',
      outline: '#717E8B',
      outlineVariant: '#CBD5DE',
    },
  },
  {
    id: 'CRIMSON_PULSE',
    name: 'Crimson',
    tagline: 'High-focus ruby & amber',
    preview: ['#A6243F', '#75565B', '#875200'],
    dark: {
      bg: '#150D0F',
      surface: '#1C1215',
      surfaceLow: '#24181B',
      surfaceHigh: '#302125',
      surfaceHighest: '#3C2B2F',
      primary: '#FFB2BC',
      onPrimary: '#660021',
      primaryContainer: '#891032',
      onPrimaryContainer: '#FFD9DE',
      secondary: '#E5BDC2',
      secondaryContainer: '#5C3F43',
      tertiary: '#F7BA64',
      tertiaryContainer: '#663E00',
      onSurface: '#F3E5E7',
      onSurfaceVariant: '#B5989D',
      outline: '#694F54',
      outlineVariant: '#3A272B',
    },
    light: {
      bg: '#FFF8F8',
      surface: '#FFFFFF',
      surfaceLow: '#FCEFF0',
      surfaceHigh: '#F5E3E5',
      surfaceHighest: '#ECD7D9',
      primary: '#A6243F',
      onPrimary: '#FFFFFF',
      primaryContainer: '#FFD9DE',
      onPrimaryContainer: '#3F0012',
      secondary: '#75565B',
      secondaryContainer: '#FFD9DE',
      tertiary: '#875200',
      tertiaryContainer: '#FFDDB3',
      onSurface: '#241215',
      onSurfaceVariant: '#61464B',
      outline: '#85676C',
      outlineVariant: '#DFC5C9',
    },
  },
];

export const REPO_URL = 'https://github.com/IMMENSE-BUILDER/Timecure';
export const APK_DOWNLOAD_URL =
  'https://github.com/IMMENSE-BUILDER/Timecure/releases/latest/download/Timecure.apk';
