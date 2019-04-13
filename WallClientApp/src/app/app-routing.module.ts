import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { AddCommentComponent } from './add-comment/add-comment.component';
import { CommentsWallComponent } from './comments-wall/comments-wall.component';

const routes: Routes = [
  { path: 'add-comment-page', component: AddCommentComponent },
  { path: 'comments-wall-page', component: CommentsWallComponent },
  { path: '', component: AddCommentComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
