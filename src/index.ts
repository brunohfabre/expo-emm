import { uniqBy } from "lodash";
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
  name: string;
  packageName: string;
  versionName: string;
  versionCode: number;
  firstInstallTime: string;
  lastUpdateTime: string;
  icon?: string;
};

interface GetInstalledPackagesProps {
  withIcon: boolean;
}

export function getInstalledPackages({
  withIcon,
}: GetInstalledPackagesProps): ApplicationType[] {
  const packages: ApplicationType[] = ExpoEmmModule.getInstalledPackages(
    withIcon,
  ).map((item: string) => {
    const [
      name,
      packageName,
      versionName,
      versionCode,
      firstInstallTime,
      lastUpdateTime,
      icon,
    ] = item.split(";");

    return {
      name,
      packageName,
      versionCode: Number(versionCode),
      versionName,
      firstInstallTime,
      lastUpdateTime,
      icon: icon ? `data:image/png;base64,${icon}` : "",
    };
  });

  const sortedPackages = packages.sort((a, b) => a.name.localeCompare(b.name));

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

export function getCallLog(): void {
  return ExpoEmmModule.getCallLog();
}

export function getIntentParam(): string {
  return ExpoEmmModule.getIntentParam();
}
