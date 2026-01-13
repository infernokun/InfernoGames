import { NgModule, inject, provideAppInitializer } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MaterialModule } from './material.module';
import { CommonModule } from '@angular/common';
import { VersionInfoComponent } from './components/version-info/version-info.component';
import { dev_log } from './utils/utils';
import { EnvironmentService } from './services/environment.service';
import { ThemeService } from './services/theme.service';
import { HttpClientModule } from '@angular/common/http';

export function init_app(environmentService: EnvironmentService) {
  return () => {
    return environmentService.load().then(() => {
      dev_log(environmentService, 'Environment loaded successfully');

      if (!environmentService.settings?.restUrl) {
        console.error('Environment loaded but REST URL is still undefined!');
        throw new Error('Failed to load environment settings');
      }
    }).then(() => {
      dev_log(environmentService, 'App initialization completed successfully');

    }).catch((error) => {
      console.error('App initialization failed:', error);
      throw error;
    });
  };
}

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    ReactiveFormsModule,
    FormsModule,
    MaterialModule,
    CommonModule,
    HttpClientModule,
    VersionInfoComponent
  ],
  providers: [
    EnvironmentService,
    ThemeService,
    provideAppInitializer(() => {
      const initializerFn = (init_app)(inject(EnvironmentService));
      return initializerFn();
    }),
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
