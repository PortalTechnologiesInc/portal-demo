import { writable } from 'svelte/store';

import { persistentStore } from './persistentStore.js';

export const FIAT_CURRENCIES = [
  { code: 'AED', label: 'AED — UAE Dirham' },
  { code: 'ANG', label: 'ANG — Netherlands Antillean Guilder' },
  { code: 'ARS', label: 'ARS — Argentine Peso' },
  { code: 'AUD', label: 'AUD — Australian Dollar' },
  { code: 'BHD', label: 'BHD — Bahraini Dinar' },
  { code: 'BRL', label: 'BRL — Brazilian Real' },
  { code: 'CAD', label: 'CAD — Canadian Dollar' },
  { code: 'CHF', label: 'CHF — Swiss Franc' },
  { code: 'CLP', label: 'CLP — Chilean Peso' },
  { code: 'CZK', label: 'CZK — Czech Koruna' },
  { code: 'DKK', label: 'DKK — Danish Krone' },
  { code: 'EUR', label: 'EUR — Euro' },
  { code: 'GBP', label: 'GBP — British Pound' },
  { code: 'HKD', label: 'HKD — Hong Kong Dollar' },
  { code: 'HUF', label: 'HUF — Hungarian Forint' },
  { code: 'IDR', label: 'IDR — Indonesian Rupiah' },
  { code: 'ILS', label: 'ILS — Israeli New Shekel' },
  { code: 'IRR', label: 'IRR — Iranian Rial' },
  { code: 'IRT', label: 'IRT — Iranian Toman' },
  { code: 'JPY', label: 'JPY — Japanese Yen' },
  { code: 'KRW', label: 'KRW — South Korean Won' },
  { code: 'KWD', label: 'KWD — Kuwaiti Dinar' },
  { code: 'LBP', label: 'LBP — Lebanese Pound' },
  { code: 'LKR', label: 'LKR — Sri Lankan Rupee' },
  { code: 'MXN', label: 'MXN — Mexican Peso' },
  { code: 'MYR', label: 'MYR — Malaysian Ringgit' },
  { code: 'NGN', label: 'NGN — Nigerian Naira' },
  { code: 'NOK', label: 'NOK — Norwegian Krone' },
  { code: 'NZD', label: 'NZD — New Zealand Dollar' },
  { code: 'PHP', label: 'PHP — Philippine Peso' },
  { code: 'PLN', label: 'PLN — Polish Zloty' },
  { code: 'RUB', label: 'RUB — Russian Ruble' },
  { code: 'SAR', label: 'SAR — Saudi Riyal' },
  { code: 'SEK', label: 'SEK — Swedish Krona' },
  { code: 'SGD', label: 'SGD — Singapore Dollar' },
  { code: 'THB', label: 'THB — Thai Baht' },
  { code: 'TRY', label: 'TRY — Turkish Lira' },
  { code: 'TWD', label: 'TWD — New Taiwan Dollar' },
  { code: 'UAH', label: 'UAH — Ukrainian Hryvnia' },
  { code: 'USD', label: 'USD — US Dollar' },
  { code: 'VEF', label: 'VEF — Venezuelan Bolívar Fuerte' },
  { code: 'VES', label: 'VES — Venezuelan Bolívar Soberano' },
  { code: 'ZAR', label: 'ZAR — South African Rand' },
];

export const loggedIn = persistentStore('loggedIn', false);

export const profile = persistentStore('state', null);
export const sessionToken = persistentStore('sessionToken', null);
export const pubkey = persistentStore('pubkey', null);

export const dashboardTab = persistentStore('dashboardTab', 'payment');

export const subscriptionsHistory = writable([]);
export const daemonVersion = writable(null);