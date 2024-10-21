import ExpoEmmModule from "./ExpoEmmModule";

export function hello(): string {
  return ExpoEmmModule.hello();
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

  return sortedPackages;
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

export function requestOverlayPermission(): void {
  return ExpoEmmModule.requestOverlayPermission();
}

export function requestPackageUsageStatsPermission(): void {
  return ExpoEmmModule.requestPackageUsageStatsPermission();
}

export function getNetworkInfo(): void {
  return ExpoEmmModule.getNetworkInfo();
}
