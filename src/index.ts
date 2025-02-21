import { uniqBy } from "lodash";
import { Subscription } from 'expo-modules-core';

import ExpoEmmModule from "./ExpoEmmModule";

export function openedByDpc(): string {
  return ExpoEmmModule.openedByDpc();
}

export function getEnrollmentSpecificId(): string {
  return ExpoEmmModule.getEnrollmentSpecificId();
}

export function getImei(index: number): string {
  return ExpoEmmModule.getImei(index);
}

export function getSerialNumber(): string {
  return ExpoEmmModule.getSerialNumber();
}

export function getDeviceId(): string {
  return ExpoEmmModule.getDeviceId();
}

export type ApplicationType = {
  label: string;
  packageName: string;
  versionName: string;
  versionCode: number;
  icon: string;
};

interface GetInstalledPackagesProps {
  withIcon: boolean;
}

export function getInstalledPackages({
  withIcon,
}: GetInstalledPackagesProps): ApplicationType[] {
  const packages: any[] = ExpoEmmModule.getInstalledPackages(
    withIcon,
  )

  if (!packages) {
    return []
  }

  const sortedPackages = packages.map(item => ({ ...item, versionCode: Number(item.versionCode) })).sort((a, b) => a.label.localeCompare(b.label));

  return uniqBy(sortedPackages, 'packageName');
}

export function launchApplication(packageName: string): string {
  return ExpoEmmModule.launchApplication(packageName);
}

export function getCurrentApp(): string {
  return ExpoEmmModule.getCurrentApp();
}

export function goToHome(): string {
  return ExpoEmmModule.goToHome();
}

export function setWallpaper(uri: string): string {
  return ExpoEmmModule.setWallpaper(uri);
}

export function verifyOverlayPermission(): boolean {
  return ExpoEmmModule.verifyOverlayPermission();
}

export function verifyPackageUsageStatsPermission(): boolean {
  return ExpoEmmModule.verifyPackageUsageStatsPermission();
}

export function verifyUnknownSourcesPermission(): boolean {
  return ExpoEmmModule.verifyUnknownSourcesPermission();
}

export function requestOverlayPermission(): void {
  return ExpoEmmModule.requestOverlayPermission();
}

export function requestPackageUsageStatsPermission(): void {
  return ExpoEmmModule.requestPackageUsageStatsPermission();
}

export function getNetworkInfo(): {
  wifi: any;
  mobile: {
    mnc: string;
    mcc: string;
    slot: string;
    iccid: string;
  }[];
} {
  return ExpoEmmModule.getNetworkInfo();
}

export function sendActivityResultOk(): string {
  return ExpoEmmModule.sendActivityResultOk();
}

export function getAppUsages(packages: string[]): { time: number; packageName: string }[] {
  const items: {
    time: string;
    packageName: string
  }[] = ExpoEmmModule.getAppUsages(packages)

  return items.map(item => ({
    time: Number(item.time) || 0,
    packageName: item.packageName
  }));
}

export function getNetworkStats(packages: string[]): {
  wifi: number,
  mobile: number,
  packageName: string,
}[] {
  const items: {
    wifi: string,
    mobile: string,
    packageName: string,
  }[] = ExpoEmmModule.getNetworkStats(packages)

  return items.map(item => ({
    wifi: Number(item.wifi) || 0,
    mobile: Number(item.mobile) || 0,
    packageName: item.packageName
  }))
}

type CallType = {
  number: string
  type: "INCOMING" | "OUTGOING" | "MISSED" | "VOICEMAIL" | "REJECTED" | "BLOCKED" | "ANSWERED_EXTERNALLY" | "UNKNOWN"
  date: string
  duration: string
}

export function getCallLog(): CallType[] {
  return ExpoEmmModule.getCallLog();
}

export function getCallLogsFromDate(fromDate: Date): CallType[] {
  return ExpoEmmModule.getCallLogsFromDate(fromDate.getTime());
}

export function getIntentParam(): string {
  return ExpoEmmModule.getIntentParam();
}

export type BatteryChangeEvent = {
  level: number
  temperature: number
  health: "GOOD" | "OVERHEAT" | "DEAD" | "OVER_VOLTAGE" | "UNSPECIFIED_FAILURE" | "COLD" | "UNKNOWN"
  isCharging: boolean
  source: "AC" | "USB" | "WIRELESS" | "DOCK" | "UNKNOWN"
  lowPowerMode: boolean
}

export function addBatteryChangeListener(listener: (event: BatteryChangeEvent) => void): Subscription {
  return ExpoEmmModule.addListener('onBatteryChange', listener);
}