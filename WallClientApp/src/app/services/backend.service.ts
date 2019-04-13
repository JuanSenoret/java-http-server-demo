import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AddCommentData } from '../models/add-comment-data.model';

@Injectable()
export class BackendService {
  private baseUrl = environment.apiUrl;
  private httpOptions = {
    headers: new HttpHeaders({
      'Content-Type':  'application/json'
    })
  };

  constructor(private httpClient: HttpClient){}

  // Call server REST API to get the comments details.
  getComments() {
    return this.httpClient.get(`${this.baseUrl}get-wall-comments`);
  }

  addComment(addCommentData: AddCommentData): Observable<AddCommentData> {
    return this.httpClient
      .post<AddCommentData>(
        `${this.baseUrl}add-wall-comment`,
        addCommentData,
        this.httpOptions
      )
      .pipe(
        map(response => {
          return response;
        })
      );
  }


  handleError(error: any) {
    console.error(error);
    return throwError(
      'Something bad happened; please try again later.');
  }

}
