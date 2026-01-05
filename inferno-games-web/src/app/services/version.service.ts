// version.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, forkJoin, of } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { APP_VERSION } from '../version';
import { BaseService } from './base.service';
import { EnvironmentService } from './environment.service';

export interface AppVersion {
  name: string;
  version: string;
}

export interface FullVersions {
  web: AppVersion;
  rest: AppVersion;
}

enum AppName {
  REST = 'rest'
}

@Injectable({
  providedIn: 'root',
})
export class VersionService extends BaseService {
  private apiUrl: string = '';
  version: string = APP_VERSION;

  constructor(
    override http: HttpClient,
    private environmentService: EnvironmentService
  ) {
    super(http);
    this.apiUrl = `${this.environmentService.settings?.restUrl}/version`;
  }

  getWebAppVersion(): AppVersion {
    return { name: 'inferno-games-web', version: this.version };
  }

  getBackendAppVersions(): Observable<ApiResponse<AppVersion>> {
    return this.get<ApiResponse<AppVersion>>(this.apiUrl);
  }

  getRest(): Observable<{ version: AppVersion; response: ApiResponse<AppVersion> }> {
    return this.getBackendAppVersions().pipe(
      map((response: ApiResponse<AppVersion>) => {
        const version = response.data || { name: AppName.REST, version: "N/A" };
        return { version, response };
      })
    );
  }

  getAllVersions(): Observable<ApiResponse<FullVersions>> {
    return forkJoin({
      web: of(this.getWebAppVersion()),
      rest: this.getRest(),
    }).pipe(
      map((result) => {
        const fullVersions: FullVersions = {
          web: result.web,
          rest: result.rest.version
        };
        
        return new ApiResponse<FullVersions>({
          code: result.rest.response.code,
          message: result.rest.response.message,
          data: fullVersions,
          type: result.rest.response.type,
          timeMs: result.rest.response.timeMs
        });
      })
    );
  }
}