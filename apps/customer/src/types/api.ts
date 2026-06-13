// Shared TypeScript types matching the backend API contracts

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

// ---- Auth ----------------------------------------------------------------

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  active: boolean;
}

// ---- Property ------------------------------------------------------------

export interface PropertySummary {
  id: string;
  title: string;
  description: string;
  address: string;
  city: string;
  pricePerNight: number;
  guestCapacity: number;
  propertyType: string;
  status: string;
  thumbnailUrl?: string;
  averageRating?: number;
  reviewCount: number;
  latitude?: number;
  longitude?: number;
  hostId?: string;
  hostFirstName?: string;
  hostLastName?: string;
}

export interface PropertyDetail extends PropertySummary {
  amenities: string[];
  photoUrls: string[];
  hostId: string;
  hostFirstName: string;
  hostLastName: string;
  status: string;
}

export interface PropertySearchRequest {
  location?: string;
  startDate?: string;
  endDate?: string;
  guests?: number;
  minPrice?: number;
  maxPrice?: number;
  propertyType?: string;
  page?: number;
  size?: number;
}

// ---- Booking -------------------------------------------------------------

export interface BookingResponse {
  id: string;
  propertyId: string;
  propertyTitle: string;
  propertyAddress: string;
  thumbnailUrl?: string;
  guestId: string;
  guestFirstName: string;
  guestLastName: string;
  checkInDate: string;
  checkOutDate: string;
  guestCount: number;
  totalPrice: number;
  platformFee: number;
  status: string;
  specialRequests?: string;
  cancellationReason?: string;
}
