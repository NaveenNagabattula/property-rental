// Admin App — Shared TypeScript types

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PropertySummary {
  id: string;
  title: string;
  city: string;
  status: string;
  hostId: string;
  hostFirstName: string;
  hostLastName: string;
  pricePerNight: number;
  propertyType: string;
  createdDate?: string;
}

export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  active: boolean;
  emailVerified: boolean;
  deleted: boolean;
}

export interface HostApplicationResponse {
  id: string;
  userId: string;
  userEmail: string;
  userFirstName: string;
  userLastName: string;
  status: string;
  reason?: string;
  createdDate: string;
}

export interface PlatformConfigResponse {
  serviceFeePercent: number;
  taxRatePercent: number;
  payoutDelayDays: number;
  cancellationPolicy: string;
}

export interface BookingResponse {
  id: string;
  propertyTitle: string;
  guestFirstName: string;
  guestLastName: string;
  checkInDate: string;
  checkOutDate: string;
  totalPrice: number;
  status: string;
}
